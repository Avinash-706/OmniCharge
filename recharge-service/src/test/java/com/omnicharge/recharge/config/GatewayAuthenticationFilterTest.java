package com.omnicharge.recharge.config;

import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;

import java.io.IOException;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class GatewayAuthenticationFilterTest {

    @Mock
    private HttpServletRequest request;

    @Mock
    private HttpServletResponse response;

    @Mock
    private FilterChain filterChain;

    private GatewayAuthenticationFilter filter;

    @BeforeEach
    void setUp() {
        filter = new GatewayAuthenticationFilter();
        SecurityContextHolder.clearContext();
    }

    @Test
    void testDoFilterInternal_WithValidHeaders() throws ServletException, IOException {
        // Given
        when(request.getHeader("X-User-Id")).thenReturn("100");
        when(request.getHeader("X-User-Role")).thenReturn("ROLE_USER");
        when(request.getHeader("X-User-Email")).thenReturn("test@example.com");

        // When
        filter.doFilterInternal(request, response, filterChain);

        // Then
        Authentication auth = SecurityContextHolder.getContext().getAuthentication();
        assertThat(auth).isNotNull();
        assertThat(auth.getPrincipal()).isEqualTo("test@example.com");
        assertThat(auth.getAuthorities()).hasSize(1);
        assertThat(auth.getAuthorities().iterator().next().getAuthority()).isEqualTo("ROLE_USER");
        
        verify(filterChain).doFilter(request, response);
    }

    @Test
    void testDoFilterInternal_WithMissingUserId() throws ServletException, IOException {
        // Given
        when(request.getHeader("X-User-Id")).thenReturn(null);
        when(request.getHeader("X-User-Role")).thenReturn("ROLE_USER");
        when(request.getHeader("X-User-Email")).thenReturn("test@example.com");

        // When
        filter.doFilterInternal(request, response, filterChain);

        // Then
        Authentication auth = SecurityContextHolder.getContext().getAuthentication();
        assertThat(auth).isNull();
        
        verify(filterChain).doFilter(request, response);
    }

    @Test
    void testDoFilterInternal_WithMissingUserRole() throws ServletException, IOException {
        // Given
        when(request.getHeader("X-User-Id")).thenReturn("100");
        when(request.getHeader("X-User-Role")).thenReturn(null);
        when(request.getHeader("X-User-Email")).thenReturn("test@example.com");

        // When
        filter.doFilterInternal(request, response, filterChain);

        // Then
        Authentication auth = SecurityContextHolder.getContext().getAuthentication();
        assertThat(auth).isNull();
        
        verify(filterChain).doFilter(request, response);
    }

    @Test
    void testDoFilterInternal_WithAdminRole() throws ServletException, IOException {
        // Given
        when(request.getHeader("X-User-Id")).thenReturn("1");
        when(request.getHeader("X-User-Role")).thenReturn("ROLE_ADMIN");
        when(request.getHeader("X-User-Email")).thenReturn("admin@example.com");

        // When
        filter.doFilterInternal(request, response, filterChain);

        // Then
        Authentication auth = SecurityContextHolder.getContext().getAuthentication();
        assertThat(auth).isNotNull();
        assertThat(auth.getPrincipal()).isEqualTo("admin@example.com");
        assertThat(auth.getAuthorities()).hasSize(1);
        assertThat(auth.getAuthorities().iterator().next().getAuthority()).isEqualTo("ROLE_ADMIN");
        
        verify(filterChain).doFilter(request, response);
    }

    @Test
    void testDoFilterInternal_WithAllHeadersMissing() throws ServletException, IOException {
        // Given
        when(request.getHeader("X-User-Id")).thenReturn(null);
        when(request.getHeader("X-User-Role")).thenReturn(null);
        when(request.getHeader("X-User-Email")).thenReturn(null);

        // When
        filter.doFilterInternal(request, response, filterChain);

        // Then
        Authentication auth = SecurityContextHolder.getContext().getAuthentication();
        assertThat(auth).isNull();
        
        verify(filterChain).doFilter(request, response);
    }
}
