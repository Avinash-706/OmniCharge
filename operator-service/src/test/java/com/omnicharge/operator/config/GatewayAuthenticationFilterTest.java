package com.omnicharge.operator.config;

import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
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

    @InjectMocks
    private GatewayAuthenticationFilter filter;

    @BeforeEach
    void setUp() {
        SecurityContextHolder.clearContext();
    }

    @Test
    void doFilterInternal_ShouldSetAuthentication_WhenHeadersPresent() throws ServletException, IOException {
        // Arrange
        when(request.getHeader("X-User-Id")).thenReturn("123");
        when(request.getHeader("X-User-Role")).thenReturn("ROLE_USER");
        when(request.getHeader("X-User-Email")).thenReturn("user@example.com");

        // Act
        filter.doFilterInternal(request, response, filterChain);

        // Assert
        Authentication auth = SecurityContextHolder.getContext().getAuthentication();
        assertThat(auth).isNotNull();
        assertThat(auth.getPrincipal()).isEqualTo("user@example.com");
        assertThat(auth.getAuthorities()).hasSize(1);
        assertThat(auth.getAuthorities().iterator().next().getAuthority()).isEqualTo("ROLE_USER");
        
        verify(filterChain).doFilter(request, response);
    }

    @Test
    void doFilterInternal_ShouldSetAdminAuthentication_WhenAdminRole() throws ServletException, IOException {
        // Arrange
        when(request.getHeader("X-User-Id")).thenReturn("456");
        when(request.getHeader("X-User-Role")).thenReturn("ROLE_ADMIN");
        when(request.getHeader("X-User-Email")).thenReturn("admin@example.com");

        // Act
        filter.doFilterInternal(request, response, filterChain);

        // Assert
        Authentication auth = SecurityContextHolder.getContext().getAuthentication();
        assertThat(auth).isNotNull();
        assertThat(auth.getPrincipal()).isEqualTo("admin@example.com");
        assertThat(auth.getAuthorities()).hasSize(1);
        assertThat(auth.getAuthorities().iterator().next().getAuthority()).isEqualTo("ROLE_ADMIN");
        
        verify(filterChain).doFilter(request, response);
    }

    @Test
    void doFilterInternal_ShouldNotSetAuthentication_WhenUserIdMissing() throws ServletException, IOException {
        // Arrange
        when(request.getHeader("X-User-Id")).thenReturn(null);
        when(request.getHeader("X-User-Role")).thenReturn("ROLE_USER");
        when(request.getHeader("X-User-Email")).thenReturn("user@example.com");

        // Act
        filter.doFilterInternal(request, response, filterChain);

        // Assert
        Authentication auth = SecurityContextHolder.getContext().getAuthentication();
        assertThat(auth).isNull();
        
        verify(filterChain).doFilter(request, response);
    }

    @Test
    void doFilterInternal_ShouldNotSetAuthentication_WhenUserRoleMissing() throws ServletException, IOException {
        // Arrange
        when(request.getHeader("X-User-Id")).thenReturn("123");
        when(request.getHeader("X-User-Role")).thenReturn(null);
        when(request.getHeader("X-User-Email")).thenReturn("user@example.com");

        // Act
        filter.doFilterInternal(request, response, filterChain);

        // Assert
        Authentication auth = SecurityContextHolder.getContext().getAuthentication();
        assertThat(auth).isNull();
        
        verify(filterChain).doFilter(request, response);
    }

    @Test
    void doFilterInternal_ShouldNotSetAuthentication_WhenAllHeadersMissing() throws ServletException, IOException {
        // Arrange
        when(request.getHeader("X-User-Id")).thenReturn(null);
        when(request.getHeader("X-User-Role")).thenReturn(null);
        when(request.getHeader("X-User-Email")).thenReturn(null);

        // Act
        filter.doFilterInternal(request, response, filterChain);

        // Assert
        Authentication auth = SecurityContextHolder.getContext().getAuthentication();
        assertThat(auth).isNull();
        
        verify(filterChain).doFilter(request, response);
    }

    @Test
    void doFilterInternal_ShouldHandleNullEmail() throws ServletException, IOException {
        // Arrange
        when(request.getHeader("X-User-Id")).thenReturn("123");
        when(request.getHeader("X-User-Role")).thenReturn("ROLE_USER");
        when(request.getHeader("X-User-Email")).thenReturn(null);

        // Act
        filter.doFilterInternal(request, response, filterChain);

        // Assert
        Authentication auth = SecurityContextHolder.getContext().getAuthentication();
        assertThat(auth).isNotNull();
        assertThat(auth.getPrincipal()).isNull();
        assertThat(auth.getAuthorities()).hasSize(1);
        
        verify(filterChain).doFilter(request, response);
    }

    @Test
    void doFilterInternal_ShouldAlwaysCallFilterChain() throws ServletException, IOException {
        // Arrange
        when(request.getHeader("X-User-Id")).thenReturn("123");
        when(request.getHeader("X-User-Role")).thenReturn("ROLE_USER");
        when(request.getHeader("X-User-Email")).thenReturn("user@example.com");

        // Act
        filter.doFilterInternal(request, response, filterChain);

        // Assert
        verify(filterChain, times(1)).doFilter(request, response);
    }

    @Test
    void doFilterInternal_ShouldClearPreviousAuthentication() throws ServletException, IOException {
        // Arrange - Set initial authentication
        SecurityContextHolder.getContext().setAuthentication(
            new org.springframework.security.authentication.UsernamePasswordAuthenticationToken(
                "old@example.com", null, java.util.Collections.emptyList()
            )
        );
        
        when(request.getHeader("X-User-Id")).thenReturn("123");
        when(request.getHeader("X-User-Role")).thenReturn("ROLE_USER");
        when(request.getHeader("X-User-Email")).thenReturn("new@example.com");

        // Act
        filter.doFilterInternal(request, response, filterChain);

        // Assert
        Authentication auth = SecurityContextHolder.getContext().getAuthentication();
        assertThat(auth).isNotNull();
        assertThat(auth.getPrincipal()).isEqualTo("new@example.com");
    }
}
