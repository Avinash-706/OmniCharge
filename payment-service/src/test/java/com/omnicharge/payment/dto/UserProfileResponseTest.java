package com.omnicharge.payment.dto;

import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.*;

class UserProfileResponseTest {

    @Test
    void testBuilder() {
        UserProfileResponse response = UserProfileResponse.builder()
                .id(1L)
                .email("user@test.com")
                .fullName("Test User")
                .mobileNumber("1234567890")
                .role("ROLE_USER")
                .createdDate("2024-01-01")
                .build();

        assertEquals(1L, response.getId());
        assertEquals("user@test.com", response.getEmail());
        assertEquals("Test User", response.getFullName());
        assertEquals("1234567890", response.getMobileNumber());
        assertEquals("ROLE_USER", response.getRole());
        assertEquals("2024-01-01", response.getCreatedDate());
    }

    @Test
    void testAllArgsConstructor() {
        UserProfileResponse response = new UserProfileResponse(
                1L, "user@test.com", "Test User", "1234567890", "ROLE_USER", "2024-01-01"
        );

        assertEquals(1L, response.getId());
        assertEquals("user@test.com", response.getEmail());
        assertEquals("Test User", response.getFullName());
    }

    @Test
    void testNoArgsConstructor() {
        UserProfileResponse response = new UserProfileResponse();
        assertNotNull(response);
    }

    @Test
    void testGettersAndSetters() {
        UserProfileResponse response = new UserProfileResponse();
        response.setId(1L);
        response.setEmail("user@test.com");
        response.setFullName("Test User");
        response.setMobileNumber("1234567890");
        response.setRole("ROLE_USER");
        response.setCreatedDate("2024-01-01");

        assertEquals(1L, response.getId());
        assertEquals("user@test.com", response.getEmail());
        assertEquals("Test User", response.getFullName());
        assertEquals("1234567890", response.getMobileNumber());
        assertEquals("ROLE_USER", response.getRole());
        assertEquals("2024-01-01", response.getCreatedDate());
    }

    @Test
    void testAdminRole() {
        UserProfileResponse response = UserProfileResponse.builder()
                .id(1L)
                .role("ROLE_ADMIN")
                .build();

        assertEquals("ROLE_ADMIN", response.getRole());
    }

    @Test
    void testNullValues() {
        UserProfileResponse response = new UserProfileResponse(null, null, null, null, null, null);
        
        assertNull(response.getId());
        assertNull(response.getEmail());
        assertNull(response.getFullName());
        assertNull(response.getMobileNumber());
        assertNull(response.getRole());
        assertNull(response.getCreatedDate());
    }
}
