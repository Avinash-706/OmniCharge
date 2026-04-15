package com.omnicharge.user.controller;

import com.omnicharge.common.dto.ApiResponse;
import com.omnicharge.common.dto.PagedResponse;
import com.omnicharge.user.dto.UserAnalyticsResponse;
import com.omnicharge.user.dto.UserProfileResponse;
import com.omnicharge.user.service.IUserService;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.format.annotation.DateTimeFormat;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

import java.time.LocalDateTime;

@RestController
@RequestMapping("/api/admin/users")
@RequiredArgsConstructor
@PreAuthorize("hasRole('ADMIN')")
public class AdminUserController {

    private final IUserService userService;

    @GetMapping
    public ResponseEntity<ApiResponse<PagedResponse<UserProfileResponse>>> getAllUsers(
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "10") int size,
            @RequestParam(defaultValue = "id") String sortBy,
            @RequestParam(defaultValue = "DESC") String sortDir) {
        
        Sort sort = sortDir.equalsIgnoreCase("ASC") ? 
                Sort.by(sortBy).ascending() : Sort.by(sortBy).descending();
        Pageable pageable = PageRequest.of(page, size, sort);
        
        Page<UserProfileResponse> usersPage = userService.getAllUsers(pageable);
        
        PagedResponse<UserProfileResponse> pagedResponse = new PagedResponse<>(
                usersPage.getContent(),
                usersPage.getNumber(),
                usersPage.getSize(),
                usersPage.getTotalElements(),
                usersPage.getTotalPages()
        );
        
        return ResponseEntity.ok(ApiResponse.success("Users retrieved successfully", pagedResponse));
    }

    @GetMapping("/{id}")
    public ResponseEntity<ApiResponse<UserProfileResponse>> getUserById(@PathVariable Long id) {
        UserProfileResponse user = userService.getUserById(id);
        return ResponseEntity.ok(ApiResponse.success("User retrieved successfully", user));
    }

    @PutMapping("/{id}/status")
    public ResponseEntity<ApiResponse<Void>> toggleUserStatus(
            @PathVariable Long id,
            @RequestParam boolean active) {
        userService.toggleUserStatus(id, active);
        return ResponseEntity.ok(ApiResponse.success("User status updated successfully", null));
    }
    
    /**
     * GET /api/admin/users/analytics?days=30
     * OR
     * GET /api/admin/users/analytics?startDate=2024-01-01T00:00:00&endDate=2024-01-31T23:59:59
     * 
     * Master BI endpoint for user growth analytics
     */
    @GetMapping("/analytics")
    public ResponseEntity<ApiResponse<UserAnalyticsResponse>> getUserAnalytics(
            @RequestParam(required = false) Integer days,
            @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE_TIME) LocalDateTime startDate,
            @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE_TIME) LocalDateTime endDate) {
        
        UserAnalyticsResponse analytics = userService.getUserAnalytics(days, startDate, endDate);
        return ResponseEntity.ok(ApiResponse.success("User analytics retrieved successfully", analytics));
    }
}
