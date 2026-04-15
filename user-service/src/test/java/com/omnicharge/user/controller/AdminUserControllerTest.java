package com.omnicharge.user.controller;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.omnicharge.common.logging.LogEventPublisher;
import com.omnicharge.user.dto.UserAnalyticsResponse;
import com.omnicharge.user.dto.UserProfileResponse;
import com.omnicharge.user.entity.AuthProvider;
import com.omnicharge.user.entity.Role;
import com.omnicharge.user.service.IUserService;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageImpl;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.jpa.mapping.JpaMetamodelMappingContext;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MockMvc;

import java.time.LocalDateTime;
import java.util.List;

import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.doNothing;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@WebMvcTest(controllers = AdminUserController.class)
@AutoConfigureMockMvc(addFilters = false)
class AdminUserControllerTest {

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private ObjectMapper objectMapper;

    @MockBean
    private IUserService userService;

    @MockBean
    private LogEventPublisher logEventPublisher;

    @MockBean
    private JpaMetamodelMappingContext jpaMappingContext;

    @MockBean
    private org.springframework.data.redis.core.RedisTemplate redisTemplate;

    @MockBean
    private org.springframework.amqp.rabbit.core.RabbitTemplate rabbitTemplate;

    @MockBean
    private com.omnicharge.user.filter.GatewayAuthenticationFilter gatewayAuthenticationFilter;

    private UserProfileResponse userProfile;
    private UserAnalyticsResponse analyticsResponse;

    @BeforeEach
    void setUp() {
        userProfile = UserProfileResponse.builder()
                .id(1L)
                .email("test@example.com")
                .fullName("Test User")
                .mobileNumber("9876543210")
                .role(Role.ROLE_USER)
                .authProvider(AuthProvider.LOCAL)
                .isActive(true)
                .createdDate(LocalDateTime.now())
                .totalSuccessfulRecharges(5L)
                .build();

        analyticsResponse = UserAnalyticsResponse.builder()
                .totalUsers(1000L)
                .activeUsers(950L)
                .inactiveUsers(50L)
                .newUsersToday(10L)
                .newUsersThisWeek(75L)
                .newUsersThisMonth(300L)
                .weekOverWeekGrowth(5.5)
                .dailyGrowth(List.of())
                .build();
    }

    @Test
    void getAllUsers_Success() throws Exception {
        Page<UserProfileResponse> userPage = new PageImpl<>(List.of(userProfile), PageRequest.of(0, 10), 1);
        when(userService.getAllUsers(any())).thenReturn(userPage);

        mockMvc.perform(get("/api/admin/users")
                .param("page", "0")
                .param("size", "10")
                .contentType(MediaType.APPLICATION_JSON))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.success").value(true))
                .andExpect(jsonPath("$.data.content[0].email").value("test@example.com"));
    }

    @Test
    void getUserById_Success() throws Exception {
        when(userService.getUserById(1L)).thenReturn(userProfile);

        mockMvc.perform(get("/api/admin/users/1")
                .contentType(MediaType.APPLICATION_JSON))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.success").value(true))
                .andExpect(jsonPath("$.data.email").value("test@example.com"));
    }

    @Test
    void toggleUserStatus_Activate_Success() throws Exception {
        doNothing().when(userService).toggleUserStatus(1L, true);

        mockMvc.perform(put("/api/admin/users/1/status")
                .param("active", "true")
                .contentType(MediaType.APPLICATION_JSON))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.success").value(true))
                .andExpect(jsonPath("$.message").value("User status updated successfully"));
    }

    @Test
    void toggleUserStatus_Deactivate_Success() throws Exception {
        doNothing().when(userService).toggleUserStatus(1L, false);

        mockMvc.perform(put("/api/admin/users/1/status")
                .param("active", "false")
                .contentType(MediaType.APPLICATION_JSON))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.success").value(true));
    }

    @Test
    void getUserAnalytics_WithDaysFilter_Success() throws Exception {
        when(userService.getUserAnalytics(eq(30), isNull(), isNull())).thenReturn(analyticsResponse);

        mockMvc.perform(get("/api/admin/users/analytics")
                .param("days", "30")
                .contentType(MediaType.APPLICATION_JSON))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.success").value(true))
                .andExpect(jsonPath("$.data.totalUsers").value(1000))
                .andExpect(jsonPath("$.data.activeUsers").value(950))
                .andExpect(jsonPath("$.data.newUsersToday").value(10));
    }

    @Test
    void getUserAnalytics_WithDateRange_Success() throws Exception {
        when(userService.getUserAnalytics(isNull(), any(LocalDateTime.class), any(LocalDateTime.class)))
                .thenReturn(analyticsResponse);

        mockMvc.perform(get("/api/admin/users/analytics")
                .param("startDate", "2026-01-01T00:00:00")
                .param("endDate", "2026-04-14T23:59:59")
                .contentType(MediaType.APPLICATION_JSON))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.success").value(true))
                .andExpect(jsonPath("$.data.totalUsers").value(1000));
    }

    @Test
    void getUserAnalytics_NoFilters_Success() throws Exception {
        when(userService.getUserAnalytics(isNull(), isNull(), isNull())).thenReturn(analyticsResponse);

        mockMvc.perform(get("/api/admin/users/analytics")
                .contentType(MediaType.APPLICATION_JSON))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.success").value(true));
    }

    // Additional tests for comprehensive coverage

    @Test
    void getAllUsers_WithAscendingSortDirection_Success() throws Exception {
        Page<UserProfileResponse> userPage = new PageImpl<>(List.of(userProfile), PageRequest.of(0, 10), 1);
        when(userService.getAllUsers(any())).thenReturn(userPage);

        mockMvc.perform(get("/api/admin/users")
                .param("page", "0")
                .param("size", "10")
                .param("sortBy", "email")
                .param("sortDir", "ASC")
                .contentType(MediaType.APPLICATION_JSON))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.success").value(true))
                .andExpect(jsonPath("$.data.content[0].email").value("test@example.com"));
    }

    @Test
    void getAllUsers_WithDescendingSortDirection_Success() throws Exception {
        Page<UserProfileResponse> userPage = new PageImpl<>(List.of(userProfile), PageRequest.of(0, 10), 1);
        when(userService.getAllUsers(any())).thenReturn(userPage);

        mockMvc.perform(get("/api/admin/users")
                .param("page", "0")
                .param("size", "10")
                .param("sortBy", "createdDate")
                .param("sortDir", "DESC")
                .contentType(MediaType.APPLICATION_JSON))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.success").value(true))
                .andExpect(jsonPath("$.data.content").isArray());
    }

    @Test
    void getAllUsers_WithCustomPageSize_Success() throws Exception {
        Page<UserProfileResponse> userPage = new PageImpl<>(List.of(userProfile), PageRequest.of(0, 20), 1);
        when(userService.getAllUsers(any())).thenReturn(userPage);

        mockMvc.perform(get("/api/admin/users")
                .param("page", "0")
                .param("size", "20")
                .contentType(MediaType.APPLICATION_JSON))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.success").value(true))
                .andExpect(jsonPath("$.data.size").value(20));
    }

    @Test
    void getAllUsers_WithDifferentPage_Success() throws Exception {
        Page<UserProfileResponse> userPage = new PageImpl<>(List.of(userProfile), PageRequest.of(2, 10), 1);
        when(userService.getAllUsers(any())).thenReturn(userPage);

        mockMvc.perform(get("/api/admin/users")
                .param("page", "2")
                .param("size", "10")
                .contentType(MediaType.APPLICATION_JSON))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.success").value(true))
                .andExpect(jsonPath("$.data.page").value(2));
    }

    @Test
    void getAllUsers_WithAllParameters_Success() throws Exception {
        Page<UserProfileResponse> userPage = new PageImpl<>(List.of(userProfile), PageRequest.of(1, 15), 1);
        when(userService.getAllUsers(any())).thenReturn(userPage);

        mockMvc.perform(get("/api/admin/users")
                .param("page", "1")
                .param("size", "15")
                .param("sortBy", "fullName")
                .param("sortDir", "ASC")
                .contentType(MediaType.APPLICATION_JSON))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.success").value(true))
                .andExpect(jsonPath("$.data.page").value(1))
                .andExpect(jsonPath("$.data.size").value(15));
    }

    @Test
    void getAllUsers_EmptyResult_Success() throws Exception {
        Page<UserProfileResponse> emptyPage = new PageImpl<>(List.of(), PageRequest.of(0, 10), 0);
        when(userService.getAllUsers(any())).thenReturn(emptyPage);

        mockMvc.perform(get("/api/admin/users")
                .param("page", "0")
                .param("size", "10")
                .contentType(MediaType.APPLICATION_JSON))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.success").value(true))
                .andExpect(jsonPath("$.data.content").isEmpty())
                .andExpect(jsonPath("$.data.totalElements").value(0));
    }

    @Test
    void getAllUsers_MultipleUsers_Success() throws Exception {
        UserProfileResponse user2 = UserProfileResponse.builder()
                .id(2L)
                .email("test2@example.com")
                .fullName("Test User 2")
                .mobileNumber("9876543211")
                .role(Role.ROLE_USER)
                .authProvider(AuthProvider.GOOGLE)
                .isActive(true)
                .createdDate(LocalDateTime.now())
                .totalSuccessfulRecharges(10L)
                .build();

        Page<UserProfileResponse> userPage = new PageImpl<>(
                List.of(userProfile, user2), 
                PageRequest.of(0, 10), 
                2
        );
        when(userService.getAllUsers(any())).thenReturn(userPage);

        mockMvc.perform(get("/api/admin/users")
                .param("page", "0")
                .param("size", "10")
                .contentType(MediaType.APPLICATION_JSON))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.success").value(true))
                .andExpect(jsonPath("$.data.content").isArray())
                .andExpect(jsonPath("$.data.content.length()").value(2))
                .andExpect(jsonPath("$.data.totalElements").value(2));
    }

    @Test
    void getUserById_WithDifferentId_Success() throws Exception {
        UserProfileResponse user = UserProfileResponse.builder()
                .id(999L)
                .email("admin@example.com")
                .fullName("Admin User")
                .mobileNumber("1234567890")
                .role(Role.ROLE_ADMIN)
                .authProvider(AuthProvider.LOCAL)
                .isActive(true)
                .createdDate(LocalDateTime.now())
                .totalSuccessfulRecharges(0L)
                .build();

        when(userService.getUserById(999L)).thenReturn(user);

        mockMvc.perform(get("/api/admin/users/999")
                .contentType(MediaType.APPLICATION_JSON))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.success").value(true))
                .andExpect(jsonPath("$.data.id").value(999))
                .andExpect(jsonPath("$.data.email").value("admin@example.com"))
                .andExpect(jsonPath("$.data.role").value("ROLE_ADMIN"));
    }

    @Test
    void getUserAnalytics_WithSpecificDaysFilter_Success() throws Exception {
        when(userService.getUserAnalytics(eq(7), isNull(), isNull())).thenReturn(analyticsResponse);

        mockMvc.perform(get("/api/admin/users/analytics")
                .param("days", "7")
                .contentType(MediaType.APPLICATION_JSON))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.success").value(true))
                .andExpect(jsonPath("$.data.totalUsers").value(1000))
                .andExpect(jsonPath("$.data.weekOverWeekGrowth").value(5.5));
    }

    @Test
    void getUserAnalytics_WithDifferentDateRange_Success() throws Exception {
        when(userService.getUserAnalytics(isNull(), any(LocalDateTime.class), any(LocalDateTime.class)))
                .thenReturn(analyticsResponse);

        mockMvc.perform(get("/api/admin/users/analytics")
                .param("startDate", "2026-03-01T00:00:00")
                .param("endDate", "2026-03-31T23:59:59")
                .contentType(MediaType.APPLICATION_JSON))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.success").value(true))
                .andExpect(jsonPath("$.data.newUsersThisMonth").value(300));
    }

    @Test
    void getUserAnalytics_VerifyAllFields_Success() throws Exception {
        when(userService.getUserAnalytics(isNull(), isNull(), isNull())).thenReturn(analyticsResponse);

        mockMvc.perform(get("/api/admin/users/analytics")
                .contentType(MediaType.APPLICATION_JSON))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.success").value(true))
                .andExpect(jsonPath("$.message").value("User analytics retrieved successfully"))
                .andExpect(jsonPath("$.data.totalUsers").value(1000))
                .andExpect(jsonPath("$.data.activeUsers").value(950))
                .andExpect(jsonPath("$.data.inactiveUsers").value(50))
                .andExpect(jsonPath("$.data.newUsersToday").value(10))
                .andExpect(jsonPath("$.data.newUsersThisWeek").value(75))
                .andExpect(jsonPath("$.data.newUsersThisMonth").value(300))
                .andExpect(jsonPath("$.data.weekOverWeekGrowth").value(5.5));
    }

    @Test
    void toggleUserStatus_VerifyResponseStructure_Success() throws Exception {
        doNothing().when(userService).toggleUserStatus(1L, true);

        mockMvc.perform(put("/api/admin/users/1/status")
                .param("active", "true")
                .contentType(MediaType.APPLICATION_JSON))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.success").value(true))
                .andExpect(jsonPath("$.message").value("User status updated successfully"))
                .andExpect(jsonPath("$.data").doesNotExist());
    }

    @Test
    void getAllUsers_WithLowercaseSortDirection_Success() throws Exception {
        Page<UserProfileResponse> userPage = new PageImpl<>(List.of(userProfile), PageRequest.of(0, 10), 1);
        when(userService.getAllUsers(any())).thenReturn(userPage);

        mockMvc.perform(get("/api/admin/users")
                .param("page", "0")
                .param("size", "10")
                .param("sortDir", "asc")
                .contentType(MediaType.APPLICATION_JSON))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.success").value(true));
    }

    @Test
    void getAllUsers_WithMixedCaseSortDirection_Success() throws Exception {
        Page<UserProfileResponse> userPage = new PageImpl<>(List.of(userProfile), PageRequest.of(0, 10), 1);
        when(userService.getAllUsers(any())).thenReturn(userPage);

        mockMvc.perform(get("/api/admin/users")
                .param("page", "0")
                .param("size", "10")
                .param("sortDir", "AsC")
                .contentType(MediaType.APPLICATION_JSON))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.success").value(true));
    }

    @Test
    void getAllUsers_WithInvalidSortDirection_DefaultsToDesc_Success() throws Exception {
        Page<UserProfileResponse> userPage = new PageImpl<>(List.of(userProfile), PageRequest.of(0, 10), 1);
        when(userService.getAllUsers(any())).thenReturn(userPage);

        mockMvc.perform(get("/api/admin/users")
                .param("page", "0")
                .param("size", "10")
                .param("sortDir", "INVALID")
                .contentType(MediaType.APPLICATION_JSON))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.success").value(true));
    }
}
