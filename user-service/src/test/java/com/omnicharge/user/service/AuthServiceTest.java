package com.omnicharge.user.service;

import com.google.api.client.googleapis.auth.oauth2.GoogleIdToken;
import com.google.api.client.googleapis.auth.oauth2.GoogleIdTokenVerifier;
import com.omnicharge.common.exception.BadRequestException;
import com.omnicharge.common.exception.DuplicateResourceException;
import com.omnicharge.common.exception.UnauthorizedException;
import com.omnicharge.common.logging.LogEventPublisher;
import com.omnicharge.user.dto.*;
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
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.data.redis.core.ValueOperations;
import org.springframework.security.crypto.password.PasswordEncoder;

import java.time.Instant;
import java.util.Optional;
import java.util.concurrent.TimeUnit;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class AuthServiceTest {

    @Mock
    private UserRepository userRepository;
    
    @Mock
    private RefreshTokenRepository refreshTokenRepository;
    
    @Mock
    private PasswordEncoder passwordEncoder;
    
    @Mock
    private JwtUtil jwtUtil;
    
    @Mock
    private GoogleIdTokenVerifier googleIdTokenVerifier;
    
    @Mock
    private RedisTemplate<String, String> redisTemplate;
    
    @Mock
    private ValueOperations<String, String> valueOperations;

    @Mock
    private LogEventPublisher logEventPublisher;

    @InjectMocks
    private AuthService authService;

    private User testUser;
    private RegisterRequest registerRequest;
    private LoginRequest loginRequest;

    @BeforeEach
    void setUp() {
        testUser = new User();
        testUser.setId(1L);
        testUser.setEmail("test@example.com");
        testUser.setFullName("Test User");
        testUser.setPassword("encodedPassword");
        testUser.setMobileNumber("9876543210");
        testUser.setRole(Role.ROLE_USER);
        testUser.setAuthProvider(AuthProvider.LOCAL);
        testUser.setIsActive(true);

        registerRequest = new RegisterRequest();
        registerRequest.setEmail("test@example.com");
        registerRequest.setFullName("Test User");
        registerRequest.setPassword("rawPassword");
        registerRequest.setMobileNumber("9876543210");

        loginRequest = new LoginRequest();
        loginRequest.setEmail("test@example.com");
        loginRequest.setPassword("rawPassword");
    }

    @Test
    void register_Success() {
        when(userRepository.existsByEmail(anyString())).thenReturn(false);
        when(userRepository.existsByMobileNumber(anyString())).thenReturn(false);
        when(passwordEncoder.encode(anyString())).thenReturn("encodedPassword");
        
        // Mock the saved user
        User savedUser = new User();
        savedUser.setId(1L);
        savedUser.setEmail(registerRequest.getEmail());
        savedUser.setFullName(registerRequest.getFullName());
        savedUser.setMobileNumber(registerRequest.getMobileNumber());
        when(userRepository.save(any(User.class))).thenReturn(savedUser);

        authService.register(registerRequest);

        verify(userRepository, times(1)).save(any(User.class));
    }

    @Test
    void register_DuplicateEmail() {
        when(userRepository.existsByEmail(anyString())).thenReturn(true);

        assertThrows(DuplicateResourceException.class, () -> authService.register(registerRequest));
        verify(userRepository, never()).save(any(User.class));
    }

    @Test
    void login_Success() {
        when(userRepository.findByEmail(anyString())).thenReturn(Optional.of(testUser));
        when(passwordEncoder.matches("rawPassword", "encodedPassword")).thenReturn(true);
        when(jwtUtil.generateAccessToken(1L, "test@example.com", "ROLE_USER", true, false, "LOCAL")).thenReturn("access-token");
        when(jwtUtil.generateRefreshToken(1L)).thenReturn("refresh-token");
        when(jwtUtil.getRefreshTokenExpiration()).thenReturn(86400000L);
        when(redisTemplate.opsForValue()).thenReturn(valueOperations);

        AuthResponse response = authService.login(loginRequest);

        assertNotNull(response);
        assertEquals("access-token", response.getAccessToken());
        assertEquals("refresh-token", response.getRefreshToken());
        verify(refreshTokenRepository, times(1)).save(any(RefreshToken.class));
    }

    @Test
    void login_InvalidPassword() {
        when(userRepository.findByEmail(anyString())).thenReturn(Optional.of(testUser));
        when(passwordEncoder.matches("rawPassword", "encodedPassword")).thenReturn(false);

        assertThrows(UnauthorizedException.class, () -> authService.login(loginRequest));
        verify(jwtUtil, never()).generateAccessToken(anyLong(), anyString(), anyString(), anyBoolean(), anyBoolean(), anyString());
    }

    @Test
    void login_GoogleUserTriesLocalLogin() {
        testUser.setAuthProvider(AuthProvider.GOOGLE);
        when(userRepository.findByEmail(anyString())).thenReturn(Optional.of(testUser));

        assertThrows(BadRequestException.class, () -> authService.login(loginRequest));
    }

    @Test
    void refreshToken_Success() {
        RefreshTokenRequest request = new RefreshTokenRequest("valid-refresh-token");
        RefreshToken tokenEntity = new RefreshToken();
        tokenEntity.setToken("valid-refresh-token");
        tokenEntity.setUser(testUser);
        tokenEntity.setExpiryDate(Instant.now().plusSeconds(3600)); // Not expired

        when(refreshTokenRepository.findByToken("valid-refresh-token")).thenReturn(Optional.of(tokenEntity));
        when(redisTemplate.opsForValue()).thenReturn(valueOperations);
        when(valueOperations.get("refresh:1:valid-refresh-token")).thenReturn("valid-refresh-token");
        when(jwtUtil.generateAccessToken(1L, "test@example.com", "ROLE_USER", true, false, "LOCAL")).thenReturn("new-access-token");

        AuthResponse response = authService.refreshToken(request);

        assertNotNull(response);
        assertEquals("new-access-token", response.getAccessToken());
    }

    @Test
    void logout_Success() {
        when(jwtUtil.extractJti("some-jwt-token")).thenReturn("token-jti");
        when(jwtUtil.getRemainingExpiration("some-jwt-token")).thenReturn(1000L);
        when(redisTemplate.opsForValue()).thenReturn(valueOperations);

        authService.logout("some-jwt-token");

        verify(valueOperations, times(1)).set("blacklist:token-jti", "true", 1000L, TimeUnit.MILLISECONDS);
    }

    @Test
    void logoutByRefreshToken_Success() {
        RefreshToken refreshToken = new RefreshToken();
        refreshToken.setToken("refresh-token-value");
        refreshToken.setUser(testUser);
        
        when(refreshTokenRepository.findByToken("refresh-token-value")).thenReturn(Optional.of(refreshToken));

        authService.logoutByRefreshToken("refresh-token-value");

        verify(redisTemplate, times(1)).delete("refresh:1:refresh-token-value");
        verify(refreshTokenRepository, times(1)).delete(refreshToken);
    }

    @Test
    void logoutByRefreshToken_TokenNotFound() {
        when(refreshTokenRepository.findByToken("invalid-token")).thenReturn(Optional.empty());

        authService.logoutByRefreshToken("invalid-token");

        verify(redisTemplate, never()).delete(anyString());
        verify(refreshTokenRepository, never()).delete(any(RefreshToken.class));
    }

    @Test
    void authenticateWithGoogle_NewUser_Success() throws Exception {
        GoogleAuthRequest request = new GoogleAuthRequest();
        request.setIdToken("valid-google-token");

        GoogleIdToken mockIdToken = mock(GoogleIdToken.class);
        GoogleIdToken.Payload mockPayload = mock(GoogleIdToken.Payload.class);
        
        when(googleIdTokenVerifier.verify("valid-google-token")).thenReturn(mockIdToken);
        when(mockIdToken.getPayload()).thenReturn(mockPayload);
        when(mockPayload.getSubject()).thenReturn("google-id-123");
        when(mockPayload.getEmail()).thenReturn("newuser@gmail.com");
        when(mockPayload.get("name")).thenReturn("New Google User");
        
        // Mock new user creation path
        when(userRepository.findByGoogleId("google-id-123")).thenReturn(Optional.empty());
        when(userRepository.existsByEmail("newuser@gmail.com")).thenReturn(false);
        
        User newUser = new User();
        newUser.setId(2L);
        newUser.setGoogleId("google-id-123");
        newUser.setEmail("newuser@gmail.com");
        newUser.setFullName("New Google User");
        newUser.setAuthProvider(AuthProvider.GOOGLE);
        newUser.setRole(Role.ROLE_USER);
        newUser.setIsActive(true);
        newUser.setIsMobileVerified(false);
        
        when(userRepository.save(any(User.class))).thenReturn(newUser);
        when(jwtUtil.generateAccessToken(anyLong(), anyString(), anyString(), anyBoolean(), anyBoolean(), anyString()))
            .thenReturn("access-token");
        when(jwtUtil.generateRefreshToken(anyLong())).thenReturn("refresh-token");
        when(jwtUtil.getRefreshTokenExpiration()).thenReturn(86400000L);
        when(jwtUtil.getAccessTokenExpiration()).thenReturn(3600000L);
        when(redisTemplate.opsForValue()).thenReturn(valueOperations);
        when(refreshTokenRepository.countByUser(any(User.class))).thenReturn(0L);

        AuthResponse response = authService.authenticateWithGoogle(request);

        assertNotNull(response);
        assertEquals("access-token", response.getAccessToken());
        assertFalse(response.getIsProfileComplete()); // No mobile number
        verify(userRepository, times(1)).save(any(User.class));
    }

    @Test
    void authenticateWithGoogle_ExistingUser_Success() throws Exception {
        GoogleAuthRequest request = new GoogleAuthRequest();
        request.setIdToken("valid-google-token");

        GoogleIdToken mockIdToken = mock(GoogleIdToken.class);
        GoogleIdToken.Payload mockPayload = mock(GoogleIdToken.Payload.class);
        
        when(googleIdTokenVerifier.verify("valid-google-token")).thenReturn(mockIdToken);
        when(mockIdToken.getPayload()).thenReturn(mockPayload);
        when(mockPayload.getSubject()).thenReturn("google-id-456");
        when(mockPayload.getEmail()).thenReturn("existing@gmail.com");
        when(mockPayload.get("name")).thenReturn("Existing User");
        
        User existingUser = new User();
        existingUser.setId(3L);
        existingUser.setGoogleId("google-id-456");
        existingUser.setEmail("existing@gmail.com");
        existingUser.setFullName("Existing User");
        existingUser.setMobileNumber("1234567890");
        existingUser.setAuthProvider(AuthProvider.GOOGLE);
        existingUser.setRole(Role.ROLE_USER);
        existingUser.setIsActive(true);
        existingUser.setIsMobileVerified(true);
        
        when(userRepository.findByGoogleId("google-id-456")).thenReturn(Optional.of(existingUser));
        when(jwtUtil.generateAccessToken(anyLong(), anyString(), anyString(), anyBoolean(), anyBoolean(), anyString()))
            .thenReturn("access-token");
        when(jwtUtil.generateRefreshToken(anyLong())).thenReturn("refresh-token");
        when(jwtUtil.getRefreshTokenExpiration()).thenReturn(86400000L);
        when(jwtUtil.getAccessTokenExpiration()).thenReturn(3600000L);
        when(redisTemplate.opsForValue()).thenReturn(valueOperations);
        when(refreshTokenRepository.countByUser(any(User.class))).thenReturn(0L);

        AuthResponse response = authService.authenticateWithGoogle(request);

        assertNotNull(response);
        assertTrue(response.getIsProfileComplete()); // Has mobile number
        assertTrue(response.getIsMobileVerified());
        verify(userRepository, never()).save(any(User.class)); // Existing user, no save
    }

    @Test
    void authenticateWithGoogle_InvalidToken() throws Exception {
        GoogleAuthRequest request = new GoogleAuthRequest();
        request.setIdToken("invalid-token");

        when(googleIdTokenVerifier.verify("invalid-token")).thenReturn(null);

        assertThrows(UnauthorizedException.class, () -> authService.authenticateWithGoogle(request));
    }

    @Test
    void authenticateWithGoogle_InactiveUser() throws Exception {
        GoogleAuthRequest request = new GoogleAuthRequest();
        request.setIdToken("valid-google-token");

        GoogleIdToken mockIdToken = mock(GoogleIdToken.class);
        GoogleIdToken.Payload mockPayload = mock(GoogleIdToken.Payload.class);
        
        when(googleIdTokenVerifier.verify("valid-google-token")).thenReturn(mockIdToken);
        when(mockIdToken.getPayload()).thenReturn(mockPayload);
        when(mockPayload.getSubject()).thenReturn("google-id-789");
        when(mockPayload.getEmail()).thenReturn("inactive@gmail.com");
        when(mockPayload.get("name")).thenReturn("Inactive User");
        
        User inactiveUser = new User();
        inactiveUser.setId(4L);
        inactiveUser.setGoogleId("google-id-789");
        inactiveUser.setEmail("inactive@gmail.com");
        inactiveUser.setIsActive(false);
        
        when(userRepository.findByGoogleId("google-id-789")).thenReturn(Optional.of(inactiveUser));

        assertThrows(UnauthorizedException.class, () -> authService.authenticateWithGoogle(request));
    }

    @Test
    void generateAuthResponse_WithMobileNumber() {
        testUser.setMobileNumber("9876543210");
        testUser.setIsMobileVerified(true);
        
        when(userRepository.findByEmail(anyString())).thenReturn(Optional.of(testUser));
        when(passwordEncoder.matches("rawPassword", "encodedPassword")).thenReturn(true);
        when(jwtUtil.generateAccessToken(1L, "test@example.com", "ROLE_USER", true, true, "LOCAL"))
            .thenReturn("access-token");
        when(jwtUtil.generateRefreshToken(1L)).thenReturn("refresh-token");
        when(jwtUtil.getRefreshTokenExpiration()).thenReturn(86400000L);
        when(jwtUtil.getAccessTokenExpiration()).thenReturn(3600000L);
        when(redisTemplate.opsForValue()).thenReturn(valueOperations);
        when(refreshTokenRepository.countByUser(testUser)).thenReturn(0L);

        AuthResponse response = authService.login(loginRequest);

        assertTrue(response.getIsProfileComplete());
        assertTrue(response.getIsMobileVerified());
    }

    @Test
    void generateAuthResponse_WithoutMobileNumber() {
        testUser.setMobileNumber(null);
        testUser.setIsMobileVerified(false);
        
        when(userRepository.findByEmail(anyString())).thenReturn(Optional.of(testUser));
        when(passwordEncoder.matches("rawPassword", "encodedPassword")).thenReturn(true);
        when(jwtUtil.generateAccessToken(1L, "test@example.com", "ROLE_USER", false, false, "LOCAL"))
            .thenReturn("access-token");
        when(jwtUtil.generateRefreshToken(1L)).thenReturn("refresh-token");
        when(jwtUtil.getRefreshTokenExpiration()).thenReturn(86400000L);
        when(jwtUtil.getAccessTokenExpiration()).thenReturn(3600000L);
        when(redisTemplate.opsForValue()).thenReturn(valueOperations);
        when(refreshTokenRepository.countByUser(testUser)).thenReturn(0L);

        AuthResponse response = authService.login(loginRequest);

        assertFalse(response.getIsProfileComplete());
        assertFalse(response.getIsMobileVerified());
    }

    @Test
    void refreshToken_ExpiredToken() {
        RefreshTokenRequest request = new RefreshTokenRequest("expired-token");
        RefreshToken tokenEntity = new RefreshToken();
        tokenEntity.setToken("expired-token");
        tokenEntity.setUser(testUser);
        tokenEntity.setExpiryDate(Instant.now().minusSeconds(3600)); // Expired

        when(refreshTokenRepository.findByToken("expired-token")).thenReturn(Optional.of(tokenEntity));

        assertThrows(UnauthorizedException.class, () -> authService.refreshToken(request));
        verify(refreshTokenRepository, times(1)).delete(tokenEntity);
    }

    @Test
    void refreshToken_TokenNotInRedis() {
        RefreshTokenRequest request = new RefreshTokenRequest("valid-token");
        RefreshToken tokenEntity = new RefreshToken();
        tokenEntity.setToken("valid-token");
        tokenEntity.setUser(testUser);
        tokenEntity.setExpiryDate(Instant.now().plusSeconds(3600));

        when(refreshTokenRepository.findByToken("valid-token")).thenReturn(Optional.of(tokenEntity));
        when(redisTemplate.opsForValue()).thenReturn(valueOperations);
        when(valueOperations.get("refresh:1:valid-token")).thenReturn(null); // Not in Redis
        when(jwtUtil.generateAccessToken(1L, "test@example.com", "ROLE_USER", true, false, "LOCAL"))
            .thenReturn("new-access-token");
        when(jwtUtil.getAccessTokenExpiration()).thenReturn(3600000L);

        AuthResponse response = authService.refreshToken(request);

        assertNotNull(response);
        assertEquals("new-access-token", response.getAccessToken());
    }

    // ========== BRANCH COVERAGE IMPROVEMENT TESTS ==========

    @Test
    void register_DuplicateMobileNumber() {
        when(userRepository.existsByEmail(anyString())).thenReturn(false);
        when(userRepository.existsByMobileNumber(anyString())).thenReturn(true);

        assertThrows(DuplicateResourceException.class, () -> authService.register(registerRequest));
        verify(userRepository, never()).save(any(User.class));
    }

    @Test
    void login_UserNotFound() {
        when(userRepository.findByEmail(anyString())).thenReturn(Optional.empty());

        assertThrows(UnauthorizedException.class, () -> authService.login(loginRequest));
        verify(passwordEncoder, never()).matches(anyString(), anyString());
    }

    @Test
    void login_InactiveUser() {
        testUser.setIsActive(false);
        when(userRepository.findByEmail(anyString())).thenReturn(Optional.of(testUser));
        when(passwordEncoder.matches("rawPassword", "encodedPassword")).thenReturn(true);

        assertThrows(UnauthorizedException.class, () -> authService.login(loginRequest));
        verify(jwtUtil, never()).generateAccessToken(anyLong(), anyString(), anyString(), anyBoolean(), anyBoolean(), anyString());
    }

    @Test
    void createGoogleUser_EmailAlreadyExists() throws Exception {
        GoogleAuthRequest request = new GoogleAuthRequest();
        request.setIdToken("valid-google-token");

        GoogleIdToken mockIdToken = mock(GoogleIdToken.class);
        GoogleIdToken.Payload mockPayload = mock(GoogleIdToken.Payload.class);
        
        when(googleIdTokenVerifier.verify("valid-google-token")).thenReturn(mockIdToken);
        when(mockIdToken.getPayload()).thenReturn(mockPayload);
        when(mockPayload.getSubject()).thenReturn("google-id-new");
        when(mockPayload.getEmail()).thenReturn("existing@example.com");
        when(mockPayload.get("name")).thenReturn("New User");
        
        when(userRepository.findByGoogleId("google-id-new")).thenReturn(Optional.empty());
        when(userRepository.existsByEmail("existing@example.com")).thenReturn(true);

        // The exception is caught and wrapped in UnauthorizedException
        assertThrows(UnauthorizedException.class, () -> authService.authenticateWithGoogle(request));
    }

    @Test
    void authenticateWithGoogle_GeneralSecurityException() throws Exception {
        GoogleAuthRequest request = new GoogleAuthRequest();
        request.setIdToken("invalid-token");

        when(googleIdTokenVerifier.verify("invalid-token"))
            .thenThrow(new java.security.GeneralSecurityException("Security error"));

        assertThrows(UnauthorizedException.class, () -> authService.authenticateWithGoogle(request));
    }

    @Test
    void authenticateWithGoogle_IOException() throws Exception {
        GoogleAuthRequest request = new GoogleAuthRequest();
        request.setIdToken("network-error-token");

        when(googleIdTokenVerifier.verify("network-error-token"))
            .thenThrow(new java.io.IOException("Network error"));

        assertThrows(UnauthorizedException.class, () -> authService.authenticateWithGoogle(request));
    }

    @Test
    void generateAuthResponse_MaxDevicesReached_FIFOEviction() {
        // Setup: User already has 4 devices (MAX_DEVICES)
        testUser.setMobileNumber("9876543210");
        
        RefreshToken oldestToken = new RefreshToken();
        oldestToken.setToken("oldest-token");
        oldestToken.setUser(testUser);
        oldestToken.setExpiryDate(Instant.now().plusSeconds(1000));
        
        RefreshToken secondToken = new RefreshToken();
        secondToken.setToken("second-token");
        secondToken.setUser(testUser);
        secondToken.setExpiryDate(Instant.now().plusSeconds(2000));
        
        when(userRepository.findByEmail(anyString())).thenReturn(Optional.of(testUser));
        when(passwordEncoder.matches("rawPassword", "encodedPassword")).thenReturn(true);
        when(refreshTokenRepository.countByUser(testUser)).thenReturn(4L); // MAX_DEVICES reached
        when(refreshTokenRepository.findByUserOrderByExpiryDateAsc(testUser))
            .thenReturn(java.util.Arrays.asList(oldestToken, secondToken));
        when(jwtUtil.generateAccessToken(1L, "test@example.com", "ROLE_USER", true, false, "LOCAL"))
            .thenReturn("access-token");
        when(jwtUtil.generateRefreshToken(1L)).thenReturn("new-refresh-token");
        when(jwtUtil.getRefreshTokenExpiration()).thenReturn(86400000L);
        when(jwtUtil.getAccessTokenExpiration()).thenReturn(3600000L);
        when(redisTemplate.opsForValue()).thenReturn(valueOperations);

        AuthResponse response = authService.login(loginRequest);

        assertNotNull(response);
        // Verify oldest token was deleted (FIFO eviction)
        verify(redisTemplate, times(1)).delete("refresh:1:oldest-token");
        verify(refreshTokenRepository, times(1)).delete(oldestToken);
    }

    @Test
    void generateAuthResponse_EmptyMobileNumber() {
        testUser.setMobileNumber(""); // Empty string
        testUser.setIsMobileVerified(false);
        
        when(userRepository.findByEmail(anyString())).thenReturn(Optional.of(testUser));
        when(passwordEncoder.matches("rawPassword", "encodedPassword")).thenReturn(true);
        when(jwtUtil.generateAccessToken(1L, "test@example.com", "ROLE_USER", false, false, "LOCAL"))
            .thenReturn("access-token");
        when(jwtUtil.generateRefreshToken(1L)).thenReturn("refresh-token");
        when(jwtUtil.getRefreshTokenExpiration()).thenReturn(86400000L);
        when(jwtUtil.getAccessTokenExpiration()).thenReturn(3600000L);
        when(redisTemplate.opsForValue()).thenReturn(valueOperations);
        when(refreshTokenRepository.countByUser(testUser)).thenReturn(0L);

        AuthResponse response = authService.login(loginRequest);

        assertFalse(response.getIsProfileComplete()); // Empty mobile = incomplete profile
    }

    @Test
    void refreshToken_InvalidToken() {
        RefreshTokenRequest request = new RefreshTokenRequest("invalid-token");
        
        when(refreshTokenRepository.findByToken("invalid-token")).thenReturn(Optional.empty());

        assertThrows(UnauthorizedException.class, () -> authService.refreshToken(request));
    }

    @Test
    void refreshToken_RedisMismatch() {
        RefreshTokenRequest request = new RefreshTokenRequest("valid-token");
        RefreshToken tokenEntity = new RefreshToken();
        tokenEntity.setToken("valid-token");
        tokenEntity.setUser(testUser);
        tokenEntity.setExpiryDate(Instant.now().plusSeconds(3600));

        when(refreshTokenRepository.findByToken("valid-token")).thenReturn(Optional.of(tokenEntity));
        when(redisTemplate.opsForValue()).thenReturn(valueOperations);
        when(valueOperations.get("refresh:1:valid-token")).thenReturn("different-token"); // Mismatch
        when(jwtUtil.generateAccessToken(1L, "test@example.com", "ROLE_USER", true, false, "LOCAL"))
            .thenReturn("new-access-token");
        when(jwtUtil.getAccessTokenExpiration()).thenReturn(3600000L);

        // Should still work (falls back to DB validation)
        AuthResponse response = authService.refreshToken(request);

        assertNotNull(response);
    }

    @Test
    void logout_ExceptionHandling() {
        when(jwtUtil.extractJti("invalid-jwt")).thenThrow(new RuntimeException("Invalid JWT"));

        // Should not throw exception (catches and logs)
        authService.logout("invalid-jwt");

        verify(valueOperations, never()).set(anyString(), anyString(), anyLong(), any(TimeUnit.class));
    }

    @Test
    void authenticateWithGoogle_ProfileIncomplete_NoMobileNumber() throws Exception {
        GoogleAuthRequest request = new GoogleAuthRequest();
        request.setIdToken("valid-google-token");

        GoogleIdToken mockIdToken = mock(GoogleIdToken.class);
        GoogleIdToken.Payload mockPayload = mock(GoogleIdToken.Payload.class);
        
        when(googleIdTokenVerifier.verify("valid-google-token")).thenReturn(mockIdToken);
        when(mockIdToken.getPayload()).thenReturn(mockPayload);
        when(mockPayload.getSubject()).thenReturn("google-id-999");
        when(mockPayload.getEmail()).thenReturn("nomobile@gmail.com");
        when(mockPayload.get("name")).thenReturn("No Mobile User");
        
        User userWithoutMobile = new User();
        userWithoutMobile.setId(5L);
        userWithoutMobile.setGoogleId("google-id-999");
        userWithoutMobile.setEmail("nomobile@gmail.com");
        userWithoutMobile.setFullName("No Mobile User");
        userWithoutMobile.setMobileNumber(null); // No mobile number
        userWithoutMobile.setAuthProvider(AuthProvider.GOOGLE);
        userWithoutMobile.setRole(Role.ROLE_USER);
        userWithoutMobile.setIsActive(true);
        userWithoutMobile.setIsMobileVerified(false);
        
        when(userRepository.findByGoogleId("google-id-999")).thenReturn(Optional.of(userWithoutMobile));
        when(jwtUtil.generateAccessToken(anyLong(), anyString(), anyString(), anyBoolean(), anyBoolean(), anyString()))
            .thenReturn("access-token");
        when(jwtUtil.generateRefreshToken(anyLong())).thenReturn("refresh-token");
        when(jwtUtil.getRefreshTokenExpiration()).thenReturn(86400000L);
        when(jwtUtil.getAccessTokenExpiration()).thenReturn(3600000L);
        when(redisTemplate.opsForValue()).thenReturn(valueOperations);
        when(refreshTokenRepository.countByUser(any(User.class))).thenReturn(0L);

        AuthResponse response = authService.authenticateWithGoogle(request);

        assertNotNull(response);
        assertFalse(response.getIsProfileComplete()); // No mobile number
    }

    @Test
    void authenticateWithGoogle_ProfileIncomplete_EmptyMobileNumber() throws Exception {
        GoogleAuthRequest request = new GoogleAuthRequest();
        request.setIdToken("valid-google-token");

        GoogleIdToken mockIdToken = mock(GoogleIdToken.class);
        GoogleIdToken.Payload mockPayload = mock(GoogleIdToken.Payload.class);
        
        when(googleIdTokenVerifier.verify("valid-google-token")).thenReturn(mockIdToken);
        when(mockIdToken.getPayload()).thenReturn(mockPayload);
        when(mockPayload.getSubject()).thenReturn("google-id-888");
        when(mockPayload.getEmail()).thenReturn("emptymobile@gmail.com");
        when(mockPayload.get("name")).thenReturn("Empty Mobile User");
        
        User userWithEmptyMobile = new User();
        userWithEmptyMobile.setId(6L);
        userWithEmptyMobile.setGoogleId("google-id-888");
        userWithEmptyMobile.setEmail("emptymobile@gmail.com");
        userWithEmptyMobile.setFullName("Empty Mobile User");
        userWithEmptyMobile.setMobileNumber(""); // Empty mobile number
        userWithEmptyMobile.setAuthProvider(AuthProvider.GOOGLE);
        userWithEmptyMobile.setRole(Role.ROLE_USER);
        userWithEmptyMobile.setIsActive(true);
        userWithEmptyMobile.setIsMobileVerified(false);
        
        when(userRepository.findByGoogleId("google-id-888")).thenReturn(Optional.of(userWithEmptyMobile));
        when(jwtUtil.generateAccessToken(anyLong(), anyString(), anyString(), anyBoolean(), anyBoolean(), anyString()))
            .thenReturn("access-token");
        when(jwtUtil.generateRefreshToken(anyLong())).thenReturn("refresh-token");
        when(jwtUtil.getRefreshTokenExpiration()).thenReturn(86400000L);
        when(jwtUtil.getAccessTokenExpiration()).thenReturn(3600000L);
        when(redisTemplate.opsForValue()).thenReturn(valueOperations);
        when(refreshTokenRepository.countByUser(any(User.class))).thenReturn(0L);

        AuthResponse response = authService.authenticateWithGoogle(request);

        assertNotNull(response);
        assertFalse(response.getIsProfileComplete()); // Empty mobile number
    }
}
