package com.omnicharge.user.service;

import com.omnicharge.common.exception.BadRequestException;
import com.omnicharge.common.exception.DuplicateResourceException;
import com.omnicharge.common.exception.ResourceNotFoundException;
import com.omnicharge.common.exception.UnauthorizedException;
import com.omnicharge.common.logging.LogEvent;
import com.omnicharge.common.logging.LogEventPublisher;
import com.omnicharge.user.dto.ChangePasswordRequest;
import com.omnicharge.user.dto.DailyUserGrowth;
import com.omnicharge.user.dto.UpdateProfileRequest;
import com.omnicharge.user.dto.UserAnalyticsResponse;
import com.omnicharge.user.dto.UserProfileResponse;
import com.omnicharge.user.entity.AuthProvider;
import com.omnicharge.user.entity.RefreshToken;
import com.omnicharge.user.entity.User;
import com.omnicharge.user.repository.RefreshTokenRepository;
import com.omnicharge.user.repository.UserRepository;
import org.springframework.data.redis.core.RedisTemplate;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
@Slf4j
public class UserService implements IUserService {

    private final UserRepository userRepository;
    private final PasswordEncoder passwordEncoder;
    private final LogEventPublisher logEventPublisher;
    private final RefreshTokenRepository refreshTokenRepository;
    private final RedisTemplate<String, String> redisTemplate;

    public UserProfileResponse getProfile(Long userId) {
        User user = userRepository.findById(userId)
                .orElseThrow(() -> new ResourceNotFoundException("User not found"));

        return mapToProfileResponse(user);
    }

    @Transactional
    public UserProfileResponse updateProfile(Long userId, UpdateProfileRequest request) {
        User user = userRepository.findById(userId)
                .orElseThrow(() -> new ResourceNotFoundException("User not found"));

        // Track changed fields
        Map<String, String> changedFields = new HashMap<>();
        
        // SECURITY LOCKDOWN: Mobile number can ONLY be updated via /verify-mobile endpoint
        // This prevents users from changing mobile numbers without verification
        
        if (!request.getFullName().equals(user.getFullName())) {
            changedFields.put("fullName", request.getFullName());
        }
        user.setFullName(request.getFullName());
        userRepository.save(user);

        log.info("Profile updated for user: {}", userId);
        
        // Log business operation
        Map<String, Object> context = new HashMap<>();
        context.put("userId", userId);
        context.put("changedFields", changedFields);
        publishBusinessLog("PROFILE_UPDATE",
            "User profile updated: userId=" + userId + ", fields=" + changedFields,
            context);
        
        return mapToProfileResponse(user);
    }

    @Transactional
    public void changePassword(Long userId, ChangePasswordRequest request) {
        User user = userRepository.findById(userId)
                .orElseThrow(() -> new ResourceNotFoundException("User not found"));

        // Verify auth provider is LOCAL
        if (user.getAuthProvider() != AuthProvider.LOCAL) {
            throw new BadRequestException("Password change is only available for manual registration accounts");
        }

        // Verify current password
        if (!passwordEncoder.matches(request.getCurrentPassword(), user.getPassword())) {
            throw new UnauthorizedException("Current password is incorrect");
        }

        // Update password
        user.setPassword(passwordEncoder.encode(request.getNewPassword()));
        userRepository.save(user);

        // SECURITY: Revoke ALL refresh tokens for this user (force logout from all devices)
        List<RefreshToken> allTokens = refreshTokenRepository.findByUserOrderByExpiryDateAsc(user);
        for (RefreshToken rt : allTokens) {
            String redisKey = "refresh:" + user.getId() + ":" + rt.getToken();
            redisTemplate.delete(redisKey);
        }
        refreshTokenRepository.deleteByUser(user);
        log.info("Password changed for user: {}. All {} refresh tokens revoked (global logout).", userId, allTokens.size());
        
        // Log business operation
        Map<String, Object> context = new HashMap<>();
        context.put("userId", userId);
        context.put("email", user.getEmail());
        context.put("devicesLoggedOut", allTokens.size());
        publishBusinessLog("PASSWORD_CHANGE_GLOBAL_LOGOUT",
            "User password changed & all sessions revoked: userId=" + userId + ", devicesLoggedOut=" + allTokens.size(),
            context);
    }

    // Admin methods
    public Page<UserProfileResponse> getAllUsers(Pageable pageable) {
        return userRepository.findAll(pageable)
                .map(this::mapToProfileResponse);
    }

    public UserProfileResponse getUserById(Long id) {
        User user = userRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("User not found"));

        return mapToProfileResponse(user);
    }

    @Transactional
    public void toggleUserStatus(Long id, boolean active) {
        User user = userRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("User not found"));

        user.setIsActive(active);
        userRepository.save(user);

        // SECURITY: If deactivating user, revoke ALL their refresh tokens immediately
        if (!active) {
            List<RefreshToken> userTokens = refreshTokenRepository.findByUserOrderByExpiryDateAsc(user);
            
            // Delete from Redis cache
            for (RefreshToken rt : userTokens) {
                String redisKey = "refresh:" + user.getId() + ":" + rt.getToken();
                redisTemplate.delete(redisKey);
            }
            
            // Delete from database
            refreshTokenRepository.deleteByUser(user);
            
            // CRITICAL: Set user deactivation flag in Redis for immediate access token invalidation
            // This ensures the API Gateway blocks requests even with valid access tokens
            String deactivationKey = "user:deactivated:" + user.getId();
            redisTemplate.opsForValue().set(deactivationKey, "true");
            // No expiry - stays until user is reactivated
            
            log.info("User {} deactivated. {} refresh tokens revoked + deactivation flag set in Redis (instant session termination).", 
                     id, userTokens.size());
            
            // Log business operation
            Map<String, Object> context = new HashMap<>();
            context.put("userId", id);
            context.put("email", user.getEmail());
            context.put("tokensRevoked", userTokens.size());
            publishBusinessLog("USER_DEACTIVATION_SESSION_REVOKE",
                "User deactivated & all sessions terminated: userId=" + id + ", tokensRevoked=" + userTokens.size(),
                context);
        } else {
            // CRITICAL: Remove deactivation flag from Redis when reactivating user
            String deactivationKey = "user:deactivated:" + user.getId();
            redisTemplate.delete(deactivationKey);
            
            log.info("User {} activated. Deactivation flag removed from Redis.", id);
        }
    }
    
    // ========== ANALYTICS IMPLEMENTATION ==========
    
    @Override
    public UserAnalyticsResponse getUserAnalytics(Integer days, LocalDateTime startDate, LocalDateTime endDate) {
        LocalDateTime now = LocalDateTime.now();
        
        // Determine date range
        LocalDateTime analyticsStartDate;
        LocalDateTime analyticsEndDate = now;
        
        if (startDate != null && endDate != null) {
            // Custom date range
            analyticsStartDate = startDate;
            analyticsEndDate = endDate;
        } else if (days != null && days > 0) {
            // Days-based filter
            analyticsStartDate = now.minusDays(days);
        } else {
            // All time default
            analyticsStartDate = LocalDateTime.of(2020, 1, 1, 0, 0);
        }
        
        LocalDateTime todayStart = now.withHour(0).withMinute(0).withSecond(0);
        LocalDateTime weekStart = now.minusDays(7);
        LocalDateTime monthStart = now.withDayOfMonth(1).withHour(0).withMinute(0).withSecond(0);
        LocalDateTime lastWeekStart = weekStart.minusDays(7);
        
        // Total counts
        long totalUsers = userRepository.count();
        long activeUsers = userRepository.countByIsActive(true);
        long inactiveUsers = userRepository.countByIsActive(false);
        
        // New users metrics
        long newUsersToday = userRepository.countByCreatedDateAfter(todayStart);
        long newUsersThisWeek = userRepository.countByCreatedDateAfter(weekStart);
        long newUsersThisMonth = userRepository.countByCreatedDateAfter(monthStart);
        
        // Week-over-Week growth
        long lastWeekUsers = userRepository.countByCreatedDateBetween(lastWeekStart, weekStart);
        double weekOverWeekGrowth = lastWeekUsers > 0 
                ? ((newUsersThisWeek - lastWeekUsers) * 100.0 / lastWeekUsers) 
                : 0.0;
        
        // Daily growth data
        List<Object[]> growthData = userRepository.findDailyUserGrowth(analyticsStartDate, analyticsEndDate);
        List<DailyUserGrowth> dailyGrowth = growthData.stream()
                .map(row -> DailyUserGrowth.builder()
                        .date(row[0].toString())
                        .newUsers((Long) row[1])
                        .build())
                .collect(Collectors.toList());
        
        log.info("User analytics generated: totalUsers={}, activeUsers={}, newUsersToday={}, dailyGrowthPoints={}", 
                totalUsers, activeUsers, newUsersToday, dailyGrowth.size());
        
        return UserAnalyticsResponse.builder()
                .totalUsers(totalUsers)
                .activeUsers(activeUsers)
                .inactiveUsers(inactiveUsers)
                .newUsersToday(newUsersToday)
                .newUsersThisWeek(newUsersThisWeek)
                .newUsersThisMonth(newUsersThisMonth)
                .weekOverWeekGrowth(weekOverWeekGrowth)
                .dailyGrowth(dailyGrowth)
                .build();
    }

    private UserProfileResponse mapToProfileResponse(User user) {
        return UserProfileResponse.builder()
                .id(user.getId())
                .email(user.getEmail())
                .fullName(user.getFullName())
                .mobileNumber(user.getMobileNumber())
                .role(user.getRole())
                .authProvider(user.getAuthProvider())
                .isActive(user.getIsActive())
                .createdDate(user.getCreatedDate())
                .totalSuccessfulRecharges(user.getTotalSuccessfulRecharges())
                .build();
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
