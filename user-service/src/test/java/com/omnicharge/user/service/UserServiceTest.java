package com.omnicharge.user.service;

import com.omnicharge.common.exception.BadRequestException;
import com.omnicharge.common.exception.DuplicateResourceException;
import com.omnicharge.common.exception.ResourceNotFoundException;
import com.omnicharge.common.exception.UnauthorizedException;
import com.omnicharge.common.logging.LogEventPublisher;
import com.omnicharge.user.dto.ChangePasswordRequest;
import com.omnicharge.user.dto.UpdateProfileRequest;
import com.omnicharge.user.dto.UserProfileResponse;
import com.omnicharge.user.entity.AuthProvider;
import com.omnicharge.user.entity.Role;
import com.omnicharge.user.entity.User;
import com.omnicharge.user.repository.UserRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageImpl;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.data.redis.core.ValueOperations;
import org.springframework.security.crypto.password.PasswordEncoder;

import java.util.List;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class UserServiceTest {

    @Mock
    private UserRepository userRepository;

    @Mock
    private PasswordEncoder passwordEncoder;

    @Mock
    private LogEventPublisher logEventPublisher;

    @Mock
    private com.omnicharge.user.repository.RefreshTokenRepository refreshTokenRepository;

    @Mock
    private org.springframework.data.redis.core.RedisTemplate<String, String> redisTemplate;

    @Mock
    private ValueOperations<String, String> valueOperations;

    @InjectMocks
    private UserService userService;

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
        testUser.setPassword("encodedPassword");
    }

    @Test
    void getProfile_Success() {
        when(userRepository.findById(1L)).thenReturn(Optional.of(testUser));

        UserProfileResponse response = userService.getProfile(1L);

        assertNotNull(response);
        assertEquals("test@example.com", response.getEmail());
        assertEquals("Test User", response.getFullName());
        verify(userRepository, times(1)).findById(1L);
    }

    @Test
    void getProfile_UserNotFound() {
        when(userRepository.findById(1L)).thenReturn(Optional.empty());

        assertThrows(ResourceNotFoundException.class, () -> userService.getProfile(1L));
        verify(userRepository, times(1)).findById(1L);
    }

    @Test
    void updateProfile_Success() {
        UpdateProfileRequest request = new UpdateProfileRequest();
        request.setFullName("Updated Name");

        when(userRepository.findById(1L)).thenReturn(Optional.of(testUser));
        when(userRepository.save(any(User.class))).thenReturn(testUser);

        UserProfileResponse response = userService.updateProfile(1L, request);

        assertNotNull(response);
        assertEquals("Updated Name", response.getFullName());
        verify(userRepository, times(1)).save(any(User.class));
    }

    @Test
    void updateProfile_UserNotFound() {
        UpdateProfileRequest request = new UpdateProfileRequest();
        request.setFullName("Updated Name");

        when(userRepository.findById(1L)).thenReturn(Optional.empty());

        assertThrows(ResourceNotFoundException.class, () -> userService.updateProfile(1L, request));
        verify(userRepository, never()).save(any(User.class));
    }

    @Test
    void changePassword_Success() {
        ChangePasswordRequest request = new ChangePasswordRequest();
        request.setCurrentPassword("oldPassword");
        request.setNewPassword("newPassword");

        when(userRepository.findById(1L)).thenReturn(Optional.of(testUser));
        when(passwordEncoder.matches("oldPassword", "encodedPassword")).thenReturn(true);
        when(passwordEncoder.encode("newPassword")).thenReturn("newEncodedPassword");

        userService.changePassword(1L, request);

        assertEquals("newEncodedPassword", testUser.getPassword());
        verify(userRepository, times(1)).save(testUser);
    }

    @Test
    void changePassword_WrongProvider() {
        testUser.setAuthProvider(AuthProvider.GOOGLE);
        ChangePasswordRequest request = new ChangePasswordRequest();

        when(userRepository.findById(1L)).thenReturn(Optional.of(testUser));

        assertThrows(BadRequestException.class, () -> userService.changePassword(1L, request));
        verify(userRepository, never()).save(any(User.class));
    }

    @Test
    void changePassword_IncorrectCurrentPassword() {
        ChangePasswordRequest request = new ChangePasswordRequest();
        request.setCurrentPassword("wrongPassword");

        when(userRepository.findById(1L)).thenReturn(Optional.of(testUser));
        when(passwordEncoder.matches("wrongPassword", "encodedPassword")).thenReturn(false);

        assertThrows(UnauthorizedException.class, () -> userService.changePassword(1L, request));
        verify(userRepository, never()).save(any(User.class));
    }

    @Test
    void getAllUsers_Success() {
        Pageable pageable = PageRequest.of(0, 10);
        Page<User> userPage = new PageImpl<>(List.of(testUser));
        
        when(userRepository.findAll(pageable)).thenReturn(userPage);

        Page<UserProfileResponse> result = userService.getAllUsers(pageable);

        assertNotNull(result);
        assertEquals(1, result.getTotalElements());
        assertEquals("test@example.com", result.getContent().get(0).getEmail());
    }

    @Test
    void toggleUserStatus_Deactivate_Success() {
        when(userRepository.findById(1L)).thenReturn(Optional.of(testUser));
        when(redisTemplate.opsForValue()).thenReturn(valueOperations);

        userService.toggleUserStatus(1L, false);

        assertFalse(testUser.getIsActive());
        verify(userRepository, times(1)).save(testUser);
        verify(redisTemplate, times(1)).opsForValue();
    }

    @Test
    void toggleUserStatus_Activate_Success() {
        testUser.setIsActive(false);
        when(userRepository.findById(1L)).thenReturn(Optional.of(testUser));

        userService.toggleUserStatus(1L, true);

        assertTrue(testUser.getIsActive());
        verify(userRepository, times(1)).save(testUser);
        verify(redisTemplate, times(1)).delete("user:deactivated:1");
    }

    @Test
    void toggleUserStatus_UserNotFound() {
        when(userRepository.findById(1L)).thenReturn(Optional.empty());

        assertThrows(ResourceNotFoundException.class, () -> userService.toggleUserStatus(1L, false));
        verify(userRepository, never()).save(any(User.class));
    }

    @Test
    void getUserById_Success() {
        when(userRepository.findById(1L)).thenReturn(Optional.of(testUser));

        UserProfileResponse response = userService.getUserById(1L);

        assertNotNull(response);
        assertEquals("test@example.com", response.getEmail());
        assertEquals("Test User", response.getFullName());
        verify(userRepository, times(1)).findById(1L);
    }

    @Test
    void getUserById_UserNotFound() {
        when(userRepository.findById(1L)).thenReturn(Optional.empty());

        assertThrows(ResourceNotFoundException.class, () -> userService.getUserById(1L));
    }

    @Test
    void getUserAnalytics_WithDaysFilter() {
        when(userRepository.count()).thenReturn(100L);
        when(userRepository.countByIsActive(true)).thenReturn(85L);
        when(userRepository.countByIsActive(false)).thenReturn(15L);
        when(userRepository.countByCreatedDateAfter(any(java.time.LocalDateTime.class))).thenReturn(5L, 20L, 45L);
        when(userRepository.countByCreatedDateBetween(any(java.time.LocalDateTime.class), any(java.time.LocalDateTime.class))).thenReturn(15L);
        
        java.util.List<Object[]> growthData = new java.util.ArrayList<>();
        growthData.add(new Object[]{"2024-01-01", 10L});
        growthData.add(new Object[]{"2024-01-02", 15L});
        when(userRepository.findDailyUserGrowth(any(java.time.LocalDateTime.class), any(java.time.LocalDateTime.class)))
            .thenReturn(growthData);

        com.omnicharge.user.dto.UserAnalyticsResponse response = userService.getUserAnalytics(30, null, null);

        assertNotNull(response);
        assertEquals(100L, response.getTotalUsers());
        assertEquals(85L, response.getActiveUsers());
        assertEquals(15L, response.getInactiveUsers());
        assertEquals(5L, response.getNewUsersToday());
        assertEquals(20L, response.getNewUsersThisWeek());
        assertEquals(45L, response.getNewUsersThisMonth());
        assertEquals(2, response.getDailyGrowth().size());
        verify(userRepository, times(1)).findDailyUserGrowth(any(java.time.LocalDateTime.class), any(java.time.LocalDateTime.class));
    }

    @Test
    void getUserAnalytics_WithCustomDateRange() {
        java.time.LocalDateTime startDate = java.time.LocalDateTime.of(2024, 1, 1, 0, 0);
        java.time.LocalDateTime endDate = java.time.LocalDateTime.of(2024, 1, 31, 23, 59);

        when(userRepository.count()).thenReturn(200L);
        when(userRepository.countByIsActive(true)).thenReturn(180L);
        when(userRepository.countByIsActive(false)).thenReturn(20L);
        when(userRepository.countByCreatedDateAfter(any(java.time.LocalDateTime.class))).thenReturn(3L, 25L, 50L);
        when(userRepository.countByCreatedDateBetween(any(java.time.LocalDateTime.class), any(java.time.LocalDateTime.class))).thenReturn(20L);
        
        java.util.List<Object[]> growthData = new java.util.ArrayList<>();
        growthData.add(new Object[]{"2024-01-15", 8L});
        when(userRepository.findDailyUserGrowth(startDate, endDate)).thenReturn(growthData);

        com.omnicharge.user.dto.UserAnalyticsResponse response = userService.getUserAnalytics(null, startDate, endDate);

        assertNotNull(response);
        assertEquals(200L, response.getTotalUsers());
        assertEquals(1, response.getDailyGrowth().size());
        verify(userRepository, times(1)).findDailyUserGrowth(startDate, endDate);
    }

    @Test
    void getUserAnalytics_AllTime() {
        when(userRepository.count()).thenReturn(500L);
        when(userRepository.countByIsActive(true)).thenReturn(450L);
        when(userRepository.countByIsActive(false)).thenReturn(50L);
        when(userRepository.countByCreatedDateAfter(any(java.time.LocalDateTime.class))).thenReturn(10L, 30L, 80L);
        when(userRepository.countByCreatedDateBetween(any(java.time.LocalDateTime.class), any(java.time.LocalDateTime.class))).thenReturn(25L);
        
        java.util.List<Object[]> growthData = new java.util.ArrayList<>();
        when(userRepository.findDailyUserGrowth(any(java.time.LocalDateTime.class), any(java.time.LocalDateTime.class)))
            .thenReturn(growthData);

        com.omnicharge.user.dto.UserAnalyticsResponse response = userService.getUserAnalytics(null, null, null);

        assertNotNull(response);
        assertEquals(500L, response.getTotalUsers());
        assertEquals(450L, response.getActiveUsers());
        assertEquals(0, response.getDailyGrowth().size());
    }

    @Test
    void getUserAnalytics_WeekOverWeekGrowthCalculation() {
        when(userRepository.count()).thenReturn(100L);
        when(userRepository.countByIsActive(true)).thenReturn(90L);
        when(userRepository.countByIsActive(false)).thenReturn(10L);
        when(userRepository.countByCreatedDateAfter(any(java.time.LocalDateTime.class))).thenReturn(2L, 30L, 60L);
        when(userRepository.countByCreatedDateBetween(any(java.time.LocalDateTime.class), any(java.time.LocalDateTime.class))).thenReturn(20L);
        
        java.util.List<Object[]> growthData = new java.util.ArrayList<>();
        when(userRepository.findDailyUserGrowth(any(java.time.LocalDateTime.class), any(java.time.LocalDateTime.class)))
            .thenReturn(growthData);

        com.omnicharge.user.dto.UserAnalyticsResponse response = userService.getUserAnalytics(7, null, null);

        assertNotNull(response);
        // Week-over-week growth: (30 - 20) * 100 / 20 = 50%
        assertEquals(50.0, response.getWeekOverWeekGrowth(), 0.01);
    }

    @Test
    void getUserAnalytics_WeekOverWeekGrowth_ZeroLastWeek() {
        when(userRepository.count()).thenReturn(100L);
        when(userRepository.countByIsActive(true)).thenReturn(90L);
        when(userRepository.countByIsActive(false)).thenReturn(10L);
        when(userRepository.countByCreatedDateAfter(any(java.time.LocalDateTime.class))).thenReturn(2L, 30L, 60L);
        when(userRepository.countByCreatedDateBetween(any(java.time.LocalDateTime.class), any(java.time.LocalDateTime.class))).thenReturn(0L);
        
        java.util.List<Object[]> growthData = new java.util.ArrayList<>();
        when(userRepository.findDailyUserGrowth(any(java.time.LocalDateTime.class), any(java.time.LocalDateTime.class)))
            .thenReturn(growthData);

        com.omnicharge.user.dto.UserAnalyticsResponse response = userService.getUserAnalytics(7, null, null);

        assertNotNull(response);
        // When last week is 0, growth should be 0
        assertEquals(0.0, response.getWeekOverWeekGrowth(), 0.01);
    }
}
