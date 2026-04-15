package com.omnicharge.payment.config;

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
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.core.context.SecurityContextHolder;

import java.io.IOException;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class GatewayAuthenticationFilterTest {

    private GatewayAuthenticationFilter filter;

    @Mock
    private HttpServletRequest request;

    @Mock
    private HttpServletResponse response;

    @Mock
    private FilterChain filterChain;

    @BeforeEach
    void setUp() {
        filter = new GatewayAuthenticationFilter();
        SecurityContextHolder.clearContext();
    }

    @Test
    void testDoFilterInternal_WithAllHeaders_ShouldSetAuthentication() throws ServletException, IOException {
        // Arrange
        when(request.getHeader("X-User-Id")).thenReturn("123");
        when(request.getHeader("X-User-Role")).thenReturn("ROLE_USER");
        when(request.getHeader("X-User-Email")).thenReturn("user@test.com");

        // Act
        filter.doFilterInternal(request, response, filterChain);

        // Assert
        Authentication auth = SecurityContextHolder.getContext().getAuthentication();
        assertNotNull(auth);
        assertEquals("user@test.com", auth.getPrincipal());
        assertTrue(auth.getAuthorities().contains(new SimpleGrantedAuthority("ROLE_USER")));
        verify(filterChain).doFilter(request, response);
    }

    @Test
    void testDoFilterInternal_WithAdminRole_ShouldSetAdminAuthority() throws ServletException, IOException {
        // Arrange
        when(request.getHeader("X-User-Id")).thenReturn("1");
        when(request.getHeader("X-User-Role")).thenReturn("ROLE_ADMIN");
        when(request.getHeader("X-User-Email")).thenReturn("admin@test.com");

        // Act
        filter.doFilterInternal(request, response, filterChain);

        // Assert
        Authentication auth = SecurityContextHolder.getContext().getAuthentication();
        assertNotNull(auth);
        assertEquals("admin@test.com", auth.getPrincipal());
        assertTrue(auth.getAuthorities().contains(new SimpleGrantedAuthority("ROLE_ADMIN")));
        verify(filterChain).doFilter(request, response);
    }

    @Test
    void testDoFilterInternal_WithoutUserId_ShouldNotSetAuthentication() throws ServletException, IOException {
        // Arrange
        when(request.getHeader("X-User-Id")).thenReturn(null);
        when(request.getHeader("X-User-Role")).thenReturn("ROLE_USER");
        when(request.getHeader("X-User-Email")).thenReturn("user@test.com");

        // Act
        filter.doFilterInternal(request, response, filterChain);

        // Assert
        Authentication auth = SecurityContextHolder.getContext().getAuthentication();
        assertNull(auth);
        verify(filterChain).doFilter(request, response);
    }

    @Test
    void testDoFilterInternal_WithoutUserRole_ShouldNotSetAuthentication() throws ServletException, IOException {
        // Arrange
        when(request.getHeader("X-User-Id")).thenReturn("123");
        when(request.getHeader("X-User-Role")).thenReturn(null);
        when(request.getHeader("X-User-Email")).thenReturn("user@test.com");

        // Act
        filter.doFilterInternal(request, response, filterChain);

        // Assert
        Authentication auth = SecurityContextHolder.getContext().getAuthentication();
        assertNull(auth);
        verify(filterChain).doFilter(request, response);
    }

    @Test
    void testDoFilterInternal_WithoutUserEmail_ShouldStillSetAuthentication() throws ServletException, IOException {
        // Arrange
        when(request.getHeader("X-User-Id")).thenReturn("123");
        when(request.getHeader("X-User-Role")).thenReturn("ROLE_USER");
        when(request.getHeader("X-User-Email")).thenReturn(null);

        // Act
        filter.doFilterInternal(request, response, filterChain);

        // Assert
        Authentication auth = SecurityContextHolder.getContext().getAuthentication();
        assertNotNull(auth);
        assertNull(auth.getPrincipal());
        assertTrue(auth.getAuthorities().contains(new SimpleGrantedAuthority("ROLE_USER")));
        verify(filterChain).doFilter(request, response);
    }

    @Test
    void testDoFilterInternal_WithNoHeaders_ShouldNotSetAuthentication() throws ServletException, IOException {
        // Arrange
        when(request.getHeader("X-User-Id")).thenReturn(null);
        when(request.getHeader("X-User-Role")).thenReturn(null);
        when(request.getHeader("X-User-Email")).thenReturn(null);

        // Act
        filter.doFilterInternal(request, response, filterChain);

        // Assert
        Authentication auth = SecurityContextHolder.getContext().getAuthentication();
        assertNull(auth);
        verify(filterChain).doFilter(request, response);
    }

    @Test
    void testDoFilterInternal_AlwaysCallsFilterChain() throws ServletException, IOException {
        // Arrange
        when(request.getHeader("X-User-Id")).thenReturn("123");
        when(request.getHeader("X-User-Role")).thenReturn("ROLE_USER");
        when(request.getHeader("X-User-Email")).thenReturn("user@test.com");

        // Act
        filter.doFilterInternal(request, response, filterChain);

        // Assert
        verify(filterChain, times(1)).doFilter(request, response);
    }

    @Test
    void testDoFilterInternal_WithEmptyHeaders_ShouldThrowException() throws ServletException, IOException {
        // Arrange
        when(request.getHeader("X-User-Id")).thenReturn("");
        when(request.getHeader("X-User-Role")).thenReturn("");
        when(request.getHeader("X-User-Email")).thenReturn("");

        // Act & Assert - Empty role string causes IllegalArgumentException in SimpleGrantedAuthority
        assertThrows(IllegalArgumentException.class, () -> {
            filter.doFilterInternal(request, response, filterChain);
        });
    }

    @Test
    void testDoFilterInternal_MultipleRoles_ShouldSetSingleAuthority() throws ServletException, IOException {
        // Arrange
        when(request.getHeader("X-User-Id")).thenReturn("123");
        when(request.getHeader("X-User-Role")).thenReturn("ROLE_USER,ROLE_ADMIN");
        when(request.getHeader("X-User-Email")).thenReturn("user@test.com");

        // Act
        filter.doFilterInternal(request, response, filterChain);

        // Assert
        Authentication auth = SecurityContextHolder.getContext().getAuthentication();
        assertNotNull(auth);
        // Filter creates single authority from the role header as-is
        assertEquals(1, auth.getAuthorities().size());
        verify(filterChain).doFilter(request, response);
    }
}
