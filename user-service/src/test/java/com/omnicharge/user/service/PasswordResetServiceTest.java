package com.omnicharge.user.service;

import com.omnicharge.common.exception.BadRequestException;
import com.omnicharge.common.exception.ResourceNotFoundException;
import com.omnicharge.common.logging.LogEventPublisher;
import com.omnicharge.user.dto.ForgotPasswordRequest;
import com.omnicharge.user.dto.ResetPasswordRequest;
import com.omnicharge.user.dto.VerifyOtpRequest;
import com.omnicharge.user.entity.AuthProvider;
import com.omnicharge.user.entity.User;
import com.omnicharge.user.repository.UserRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.data.redis.core.ValueOperations;
import org.springframework.security.crypto.password.PasswordEncoder;

import java.util.Optional;
import java.util.concurrent.TimeUnit;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class PasswordResetServiceTest {

    @Mock
    private UserRepository userRepository;

    @Mock
    private IEmailService emailService;

    @Mock
    private RedisTemplate<String, String> redisTemplate;

    @Mock
    private PasswordEncoder passwordEncoder;

    @Mock
    private ValueOperations<String, String> valueOperations;

    @Mock
    private LogEventPublisher logEventPublisher;

    @Mock
    private org.springframework.amqp.rabbit.core.RabbitTemplate rabbitTemplate;

    @InjectMocks
    private PasswordResetService passwordResetService;

    private User testUser;
    
    @BeforeEach
    void setUp() {
        testUser = new User();
        testUser.setId(1L);
        testUser.setEmail("test@example.com");
        testUser.setPassword("oldEncodedPassword");
        testUser.setAuthProvider(AuthProvider.LOCAL);
        testUser.setMobileNumber("9876543210"); // Add mobile number
        testUser.setIsMobileVerified(true); // Set mobile as verified (required for password reset)
    }

    @Test
    void forgotPassword_Success() {
        ForgotPasswordRequest request = new ForgotPasswordRequest("test@example.com");

        when(userRepository.findByEmail("test@example.com")).thenReturn(Optional.of(testUser));
        when(redisTemplate.opsForValue()).thenReturn(valueOperations);

        passwordResetService.forgotPassword(request);

        verify(rabbitTemplate, times(1)).convertAndSend(anyString(), anyString(), any(Object.class));
        verify(valueOperations, times(1)).set(eq("password-reset-otp:test@example.com"), anyString(), eq(5L), eq(TimeUnit.MINUTES));
    }

    @Test
    void forgotPassword_GoogleProviderThrowsException() {
        testUser.setAuthProvider(AuthProvider.GOOGLE);
        ForgotPasswordRequest request = new ForgotPasswordRequest("test@example.com");

        when(userRepository.findByEmail("test@example.com")).thenReturn(Optional.of(testUser));

        assertThrows(BadRequestException.class, () -> passwordResetService.forgotPassword(request));
        verify(emailService, never()).sendOtpEmail(anyString(), anyString());
    }

    @Test
    void verifyOtp_Success() {
        VerifyOtpRequest request = new VerifyOtpRequest("test@example.com", "123456");

        when(redisTemplate.opsForValue()).thenReturn(valueOperations);
        when(valueOperations.get("password-reset-otp:test@example.com")).thenReturn("123456");

        boolean result = passwordResetService.verifyOtp(request);

        assertTrue(result);
    }

    @Test
    void verifyOtp_ExpiredOrMissing() {
        VerifyOtpRequest request = new VerifyOtpRequest("test@example.com", "123456");

        when(redisTemplate.opsForValue()).thenReturn(valueOperations);
        when(valueOperations.get("password-reset-otp:test@example.com")).thenReturn(null);

        assertThrows(BadRequestException.class, () -> passwordResetService.verifyOtp(request));
    }

    @Test
    void verifyOtp_InvalidOtp() {
        VerifyOtpRequest request = new VerifyOtpRequest("test@example.com", "123456");

        when(redisTemplate.opsForValue()).thenReturn(valueOperations);
        when(valueOperations.get("password-reset-otp:test@example.com")).thenReturn("654321"); // Mismatch

        assertThrows(BadRequestException.class, () -> passwordResetService.verifyOtp(request));
    }

    @Test
    void resetPassword_Success() {
        ResetPasswordRequest request = new ResetPasswordRequest();
        request.setEmail("test@example.com");
        request.setOtp("123456");
        request.setNewPassword("newPassword");

        when(redisTemplate.opsForValue()).thenReturn(valueOperations);
        when(valueOperations.get("password-reset-otp:test@example.com")).thenReturn("123456"); // Validates OTP successfully
        when(userRepository.findByEmail("test@example.com")).thenReturn(Optional.of(testUser));
        when(passwordEncoder.encode("newPassword")).thenReturn("newEncodedPassword");

        passwordResetService.resetPassword(request);

        assertEquals("newEncodedPassword", testUser.getPassword());
        verify(userRepository, times(1)).save(testUser);
        verify(redisTemplate, times(1)).delete("password-reset-otp:test@example.com"); // Reaps the OTP after use
    }

    // BRANCH COVERAGE TESTS - Missing branches

    @Test
    void forgotPassword_UserNotFound() {
        ForgotPasswordRequest request = new ForgotPasswordRequest("nonexistent@example.com");

        when(userRepository.findByEmail("nonexistent@example.com")).thenReturn(Optional.empty());

        assertThrows(ResourceNotFoundException.class, () -> passwordResetService.forgotPassword(request));
        verify(rabbitTemplate, never()).convertAndSend(anyString(), anyString(), any(Object.class));
    }

    @Test
    void forgotPassword_NullMobileNumber() {
        testUser.setMobileNumber(null);
        ForgotPasswordRequest request = new ForgotPasswordRequest("test@example.com");

        when(userRepository.findByEmail("test@example.com")).thenReturn(Optional.of(testUser));

        assertThrows(BadRequestException.class, () -> passwordResetService.forgotPassword(request));
        verify(rabbitTemplate, never()).convertAndSend(anyString(), anyString(), any(Object.class));
    }

    @Test
    void forgotPassword_EmptyMobileNumber() {
        testUser.setMobileNumber("");
        ForgotPasswordRequest request = new ForgotPasswordRequest("test@example.com");

        when(userRepository.findByEmail("test@example.com")).thenReturn(Optional.of(testUser));

        assertThrows(BadRequestException.class, () -> passwordResetService.forgotPassword(request));
        verify(rabbitTemplate, never()).convertAndSend(anyString(), anyString(), any(Object.class));
    }

    @Test
    void forgotPassword_MobileNotVerified() {
        testUser.setIsMobileVerified(false);
        ForgotPasswordRequest request = new ForgotPasswordRequest("test@example.com");

        when(userRepository.findByEmail("test@example.com")).thenReturn(Optional.of(testUser));

        assertThrows(BadRequestException.class, () -> passwordResetService.forgotPassword(request));
        verify(rabbitTemplate, never()).convertAndSend(anyString(), anyString(), any(Object.class));
    }

    @Test
    void forgotPassword_MobileNotVerified_NullCheck() {
        testUser.setIsMobileVerified(null);
        ForgotPasswordRequest request = new ForgotPasswordRequest("test@example.com");

        when(userRepository.findByEmail("test@example.com")).thenReturn(Optional.of(testUser));

        assertThrows(BadRequestException.class, () -> passwordResetService.forgotPassword(request));
        verify(rabbitTemplate, never()).convertAndSend(anyString(), anyString(), any(Object.class));
    }

    @Test
    void resetPassword_UserNotFound() {
        ResetPasswordRequest request = new ResetPasswordRequest();
        request.setEmail("nonexistent@example.com");
        request.setOtp("123456");
        request.setNewPassword("newPassword");

        when(redisTemplate.opsForValue()).thenReturn(valueOperations);
        when(valueOperations.get("password-reset-otp:nonexistent@example.com")).thenReturn("123456");
        when(userRepository.findByEmail("nonexistent@example.com")).thenReturn(Optional.empty());

        assertThrows(ResourceNotFoundException.class, () -> passwordResetService.resetPassword(request));
        verify(userRepository, never()).save(any(User.class));
    }

    @Test
    void resetPassword_GoogleProviderThrowsException() {
        testUser.setAuthProvider(AuthProvider.GOOGLE);
        ResetPasswordRequest request = new ResetPasswordRequest();
        request.setEmail("test@example.com");
        request.setOtp("123456");
        request.setNewPassword("newPassword");

        when(redisTemplate.opsForValue()).thenReturn(valueOperations);
        when(valueOperations.get("password-reset-otp:test@example.com")).thenReturn("123456");
        when(userRepository.findByEmail("test@example.com")).thenReturn(Optional.of(testUser));

        assertThrows(BadRequestException.class, () -> passwordResetService.resetPassword(request));
        verify(userRepository, never()).save(any(User.class));
    }
}
