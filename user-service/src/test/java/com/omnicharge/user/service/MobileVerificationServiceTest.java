package com.omnicharge.user.service;

import com.omnicharge.common.exception.BadRequestException;
import com.omnicharge.common.exception.DuplicateResourceException;
import com.omnicharge.common.exception.ResourceNotFoundException;
import com.omnicharge.common.logging.LogEventPublisher;
import com.omnicharge.user.dto.AuthResponse;
import com.omnicharge.user.dto.SendMobileOtpRequest;
import com.omnicharge.user.dto.VerifyMobileOtpRequest;
import com.omnicharge.user.entity.AuthProvider;
import com.omnicharge.user.entity.RefreshToken;
import com.omnicharge.user.entity.Role;
import com.omnicharge.user.entity.User;
import com.omnicharge.user.repository.RefreshTokenRepository;
import com.omnicharge.user.repository.UserRepository;
import com.omnicharge.user.util.JwtUtil;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.amqp.rabbit.core.RabbitTemplate;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.data.redis.core.ValueOperations;

import java.util.Optional;
import java.util.concurrent.TimeUnit;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class MobileVerificationServiceTest {

    @Mock
    private UserRepository userRepository;

    @Mock
    private RefreshTokenRepository refreshTokenRepository;

    @Mock
    private JwtUtil jwtUtil;

    @Mock
    private RedisTemplate<String, String> redisTemplate;

    @Mock
    private ValueOperations<String, String> valueOperations;

    @Mock
    private RabbitTemplate rabbitTemplate;

    @Mock
    private LogEventPublisher logEventPublisher;

    @InjectMocks
    private MobileVerificationService mobileVerificationService;

    private User testUser;

    @BeforeEach
    void setUp() {
        testUser = new User();
        testUser.setId(1L);
        testUser.setEmail("test@example.com");
        testUser.setFullName("Test User");
        testUser.setMobileNumber("9876543210");
        testUser.setRole(Role.ROLE_USER);
        testUser.setAuthProvider(AuthProvider.LOCAL);
        testUser.setIsActive(true);
        testUser.setIsMobileVerified(false);
    }

    @Test
    void sendOtp_Success() {
        SendMobileOtpRequest request = new SendMobileOtpRequest();
        request.setMobileNumber("9876543210");

        when(userRepository.findById(1L)).thenReturn(Optional.of(testUser));
        when(userRepository.findByMobileNumber("9876543210")).thenReturn(Optional.empty());
        when(redisTemplate.opsForValue()).thenReturn(valueOperations);

        mobileVerificationService.sendOtp(1L, request);

        verify(valueOperations, times(1)).set(eq("mobile-otp:1"), anyString(), eq(5L), eq(TimeUnit.MINUTES));
        verify(valueOperations, times(1)).set(eq("mobile-otp-num:1"), eq("9876543210"), eq(5L), eq(TimeUnit.MINUTES));
        verify(rabbitTemplate, times(1)).convertAndSend(eq("omnicharge.exchange"), eq("mobile.otp.send"), any(Object.class));
    }

    @Test
    void sendOtp_UserNotFound() {
        SendMobileOtpRequest request = new SendMobileOtpRequest();
        request.setMobileNumber("9876543210");

        when(userRepository.findById(1L)).thenReturn(Optional.empty());

        assertThrows(ResourceNotFoundException.class, () -> mobileVerificationService.sendOtp(1L, request));
        verify(rabbitTemplate, never()).convertAndSend(anyString(), anyString(), any(Object.class));
    }

    @Test
    void sendOtp_AlreadyVerified() {
        testUser.setIsMobileVerified(true);
        SendMobileOtpRequest request = new SendMobileOtpRequest();
        request.setMobileNumber("9876543210");

        when(userRepository.findById(1L)).thenReturn(Optional.of(testUser));

        assertThrows(BadRequestException.class, () -> mobileVerificationService.sendOtp(1L, request));
        verify(rabbitTemplate, never()).convertAndSend(anyString(), anyString(), any(Object.class));
    }

    @Test
    void sendOtp_MobileNumberAlreadyUsedByAnotherUser() {
        SendMobileOtpRequest request = new SendMobileOtpRequest();
        request.setMobileNumber("9999999999");

        User anotherUser = new User();
        anotherUser.setId(2L);
        anotherUser.setMobileNumber("9999999999");
        anotherUser.setIsMobileVerified(true);

        when(userRepository.findById(1L)).thenReturn(Optional.of(testUser));
        when(userRepository.findByMobileNumber("9999999999")).thenReturn(Optional.of(anotherUser));

        assertThrows(DuplicateResourceException.class, () -> mobileVerificationService.sendOtp(1L, request));
        verify(rabbitTemplate, never()).convertAndSend(anyString(), anyString(), any(Object.class));
    }

    @Test
    void sendOtp_SameUserUpdatingOwnUnverifiedMobile_Success() {
        // User trying to update their own unverified mobile number - should be allowed
        testUser.setMobileNumber("9876543210");
        testUser.setIsMobileVerified(false);
        
        SendMobileOtpRequest request = new SendMobileOtpRequest();
        request.setMobileNumber("9999999999"); // Different number

        when(userRepository.findById(1L)).thenReturn(Optional.of(testUser));
        when(userRepository.findByMobileNumber("9999999999")).thenReturn(Optional.of(testUser)); // Same user
        when(redisTemplate.opsForValue()).thenReturn(valueOperations);

        mobileVerificationService.sendOtp(1L, request);

        verify(valueOperations, times(1)).set(eq("mobile-otp:1"), anyString(), eq(5L), eq(TimeUnit.MINUTES));
        verify(rabbitTemplate, times(1)).convertAndSend(eq("omnicharge.exchange"), eq("mobile.otp.send"), any(Object.class));
    }

    @Test
    void verifyOtp_Success() {
        VerifyMobileOtpRequest request = new VerifyMobileOtpRequest();
        request.setMobileNumber("9876543210");
        request.setOtp("123456");

        when(userRepository.findById(1L)).thenReturn(Optional.of(testUser));
        when(redisTemplate.opsForValue()).thenReturn(valueOperations);
        when(valueOperations.get("mobile-otp:1")).thenReturn("123456");
        when(valueOperations.get("mobile-otp-num:1")).thenReturn("9876543210");
        when(userRepository.findByMobileNumber("9876543210")).thenReturn(Optional.empty());
        when(userRepository.save(any(User.class))).thenReturn(testUser);
        when(jwtUtil.generateAccessToken(anyLong(), anyString(), anyString(), anyBoolean(), anyBoolean(), anyString()))
                .thenReturn("access-token");
        when(jwtUtil.generateRefreshToken(anyLong())).thenReturn("refresh-token");
        when(jwtUtil.getAccessTokenExpiration()).thenReturn(3600000L);
        when(jwtUtil.getRefreshTokenExpiration()).thenReturn(86400000L);

        AuthResponse response = mobileVerificationService.verifyOtp(1L, request);

        assertNotNull(response);
        assertEquals("access-token", response.getAccessToken());
        assertTrue(response.getIsMobileVerified());
        verify(userRepository, times(1)).save(any(User.class));
        verify(redisTemplate, times(1)).delete("mobile-otp:1");
        verify(redisTemplate, times(1)).delete("mobile-otp-num:1");
    }

    @Test
    void verifyOtp_OtpExpired() {
        VerifyMobileOtpRequest request = new VerifyMobileOtpRequest();
        request.setMobileNumber("9876543210");
        request.setOtp("123456");

        when(userRepository.findById(1L)).thenReturn(Optional.of(testUser));
        when(redisTemplate.opsForValue()).thenReturn(valueOperations);
        when(valueOperations.get("mobile-otp:1")).thenReturn(null);

        assertThrows(BadRequestException.class, () -> mobileVerificationService.verifyOtp(1L, request));
        verify(userRepository, never()).save(any(User.class));
    }

    @Test
    void verifyOtp_MobileNumberMismatch() {
        VerifyMobileOtpRequest request = new VerifyMobileOtpRequest();
        request.setMobileNumber("9999999999");
        request.setOtp("123456");

        when(userRepository.findById(1L)).thenReturn(Optional.of(testUser));
        when(redisTemplate.opsForValue()).thenReturn(valueOperations);
        when(valueOperations.get("mobile-otp:1")).thenReturn("123456");
        when(valueOperations.get("mobile-otp-num:1")).thenReturn("9876543210");

        assertThrows(BadRequestException.class, () -> mobileVerificationService.verifyOtp(1L, request));
        verify(userRepository, never()).save(any(User.class));
    }

    @Test
    void verifyOtp_InvalidOtp() {
        VerifyMobileOtpRequest request = new VerifyMobileOtpRequest();
        request.setMobileNumber("9876543210");
        request.setOtp("654321");

        when(userRepository.findById(1L)).thenReturn(Optional.of(testUser));
        when(redisTemplate.opsForValue()).thenReturn(valueOperations);
        when(valueOperations.get("mobile-otp:1")).thenReturn("123456");
        when(valueOperations.get("mobile-otp-num:1")).thenReturn("9876543210");

        assertThrows(BadRequestException.class, () -> mobileVerificationService.verifyOtp(1L, request));
        verify(userRepository, never()).save(any(User.class));
    }

    @Test
    void verifyOtp_RaceCondition_MobileAlreadyTakenByAnotherUser() {
        VerifyMobileOtpRequest request = new VerifyMobileOtpRequest();
        request.setMobileNumber("9876543210");
        request.setOtp("123456");

        User anotherUser = new User();
        anotherUser.setId(2L);
        anotherUser.setMobileNumber("9876543210");
        anotherUser.setIsMobileVerified(true);

        when(userRepository.findById(1L)).thenReturn(Optional.of(testUser));
        when(redisTemplate.opsForValue()).thenReturn(valueOperations);
        when(valueOperations.get("mobile-otp:1")).thenReturn("123456");
        when(valueOperations.get("mobile-otp-num:1")).thenReturn("9876543210");
        when(userRepository.findByMobileNumber("9876543210")).thenReturn(Optional.of(anotherUser));

        assertThrows(DuplicateResourceException.class, () -> mobileVerificationService.verifyOtp(1L, request));
        verify(userRepository, never()).save(any(User.class));
    }
}
