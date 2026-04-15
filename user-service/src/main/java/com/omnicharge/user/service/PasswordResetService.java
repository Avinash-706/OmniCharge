package com.omnicharge.user.service;

import com.omnicharge.common.exception.BadRequestException;
import com.omnicharge.common.exception.ResourceNotFoundException;
import com.omnicharge.common.logging.LogEvent;
import com.omnicharge.common.logging.LogEventPublisher;
import com.omnicharge.user.dto.ForgotPasswordRequest;
import com.omnicharge.user.dto.OtpEvent;
import com.omnicharge.user.dto.ResetPasswordRequest;
import com.omnicharge.user.dto.VerifyOtpRequest;
import com.omnicharge.user.entity.AuthProvider;
import com.omnicharge.user.entity.User;
import com.omnicharge.user.repository.UserRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.amqp.rabbit.core.RabbitTemplate;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.HashMap;
import java.util.Map;
import java.util.Random;
import java.util.concurrent.TimeUnit;

@Service
@RequiredArgsConstructor
@Slf4j
public class PasswordResetService implements IPasswordResetService {

    private final UserRepository userRepository;
    private final RedisTemplate<String, String> redisTemplate;
    private final PasswordEncoder passwordEncoder;
    private final LogEventPublisher logEventPublisher;
    private final RabbitTemplate rabbitTemplate;

    private static final long OTP_EXPIRATION_MINUTES = 5;
    private static final String EXCHANGE_NAME = "omnicharge.exchange";
    private static final String ROUTING_KEY_OTP = "mobile.otp.send";

    public void forgotPassword(ForgotPasswordRequest request) {
        // Find user by email
        User user = userRepository.findByEmail(request.getEmail())
                .orElseThrow(() -> new ResourceNotFoundException("User not found with email: " + request.getEmail()));

        // CRITICAL: Verify auth provider is LOCAL - Google users cannot reset passwords
        if (user.getAuthProvider() != AuthProvider.LOCAL) {
            throw new BadRequestException("Google accounts cannot reset passwords here. Please use Google Sign-In.");
        }

        // CRITICAL: Verify user has a mobile number
        if (user.getMobileNumber() == null || user.getMobileNumber().isEmpty()) {
            throw new BadRequestException("No mobile number linked to this account. Please contact support.");
        }

        // CRITICAL: Verify mobile number is verified
        if (!Boolean.TRUE.equals(user.getIsMobileVerified())) {
            throw new BadRequestException("Mobile number not verified. Please verify your mobile number first.");
        }

        // Generate 6-digit OTP
        String otp = generateOtp();

        // Store OTP in Redis with 5-minute expiration
        String redisKey = "password-reset-otp:" + request.getEmail();
        redisTemplate.opsForValue().set(
                redisKey,
                otp,
                OTP_EXPIRATION_MINUTES,
                TimeUnit.MINUTES
        );

        // UPGRADED: Send OTP via Twilio SMS (publish to RabbitMQ)
        OtpEvent event = OtpEvent.builder()
                .userId(user.getId())
                .mobileNumber(user.getMobileNumber())
                .otp(otp)
                .build();
                
        rabbitTemplate.convertAndSend(EXCHANGE_NAME, ROUTING_KEY_OTP, event);
        log.info("Password reset OTP sent to RabbitMQ for mobile: {}", user.getMobileNumber());
        
        // Log business operation
        Map<String, Object> context = new HashMap<>();
        context.put("userId", user.getId());
        context.put("email", request.getEmail());
        context.put("mobileNumber", user.getMobileNumber());
        context.put("isMobileVerified", user.getIsMobileVerified());
        publishBusinessLog("PASSWORD_RESET_REQUEST",
            "Password reset OTP requested via SMS: userId=" + user.getId(),
            context);
    }

    public boolean verifyOtp(VerifyOtpRequest request) {
        String redisKey = "password-reset-otp:" + request.getEmail();
        String storedOtp = redisTemplate.opsForValue().get(redisKey);

        if (storedOtp == null) {
            throw new BadRequestException("OTP expired or not found");
        }

        if (!storedOtp.equals(request.getOtp())) {
            throw new BadRequestException("Invalid OTP");
        }

        log.info("Password reset OTP verified successfully for email: {}", request.getEmail());
        return true;
    }

    @Transactional
    public void resetPassword(ResetPasswordRequest request) {
        // Verify OTP first
        VerifyOtpRequest verifyRequest = new VerifyOtpRequest(request.getEmail(), request.getOtp());
        verifyOtp(verifyRequest);

        // Find user
        User user = userRepository.findByEmail(request.getEmail())
                .orElseThrow(() -> new ResourceNotFoundException("User not found"));

        // Verify auth provider is LOCAL
        if (user.getAuthProvider() != AuthProvider.LOCAL) {
            throw new BadRequestException("Password reset is only available for manual registration accounts");
        }

        // Update password
        user.setPassword(passwordEncoder.encode(request.getNewPassword()));
        userRepository.save(user);

        // Delete OTP from Redis
        String redisKey = "password-reset-otp:" + request.getEmail();
        redisTemplate.delete(redisKey);

        log.info("Password reset successfully for email: {}", request.getEmail());
        
        // Log business operation
        Map<String, Object> context = new HashMap<>();
        context.put("userId", user.getId());
        context.put("email", request.getEmail());
        publishBusinessLog("PASSWORD_RESET_COMPLETE",
            "Password reset completed: userId=" + user.getId(),
            context);
    }

    private String generateOtp() {
        Random random = new Random();
        int otp = 100000 + random.nextInt(900000); // 6-digit OTP
        return String.valueOf(otp);
    }
    
    // Helper method for business operation logging
    private void publishBusinessLog(String eventType, String message, Map<String, Object> context) {
        LogEvent logEvent = LogEvent.builder()
                .serviceName("user-service")
                .level("INFO")
                .logger(this.getClass().getName())
                .message(message)
                .eventType(eventType)
                .context(context)
                .timestamp(LocalDateTime.now())
                .build();
        logEventPublisher.publish(logEvent);
    }
}
