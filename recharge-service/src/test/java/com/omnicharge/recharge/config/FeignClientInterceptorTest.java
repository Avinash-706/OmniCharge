package com.omnicharge.recharge.config;

import feign.RequestTemplate;
import jakarta.servlet.http.HttpServletRequest;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.web.context.request.RequestContextHolder;
import org.springframework.web.context.request.ServletRequestAttributes;

import java.util.Collection;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class FeignClientInterceptorTest {

    @Mock
    private HttpServletRequest request;

    private FeignClientInterceptor interceptor;
    private RequestTemplate template;

    @BeforeEach
    void setUp() {
        interceptor = new FeignClientInterceptor();
        template = new RequestTemplate();
        RequestContextHolder.resetRequestAttributes();
    }

    @Test
    void testApply_WithAllHeaders() {
        // Given
        ServletRequestAttributes attributes = new ServletRequestAttributes(request);
        RequestContextHolder.setRequestAttributes(attributes);
        
        when(request.getHeader("X-User-Id")).thenReturn("100");
        when(request.getHeader("X-User-Role")).thenReturn("ROLE_USER");
        when(request.getHeader("X-User-Email")).thenReturn("test@example.com");

        // When
        interceptor.apply(template);

        // Then
        Collection<String> userIdHeaders = template.headers().get("X-User-Id");
        Collection<String> userRoleHeaders = template.headers().get("X-User-Role");
        Collection<String> userEmailHeaders = template.headers().get("X-User-Email");
        
        assertThat(userIdHeaders).containsExactly("100");
        assertThat(userRoleHeaders).containsExactly("ROLE_USER");
        assertThat(userEmailHeaders).containsExactly("test@example.com");
    }

    @Test
    void testApply_WithMissingHeaders() {
        // Given
        ServletRequestAttributes attributes = new ServletRequestAttributes(request);
        RequestContextHolder.setRequestAttributes(attributes);
        
        when(request.getHeader("X-User-Id")).thenReturn(null);
        when(request.getHeader("X-User-Role")).thenReturn(null);
        when(request.getHeader("X-User-Email")).thenReturn(null);

        // When
        interceptor.apply(template);

        // Then
        assertThat(template.headers().get("X-User-Id")).isNull();
        assertThat(template.headers().get("X-User-Role")).isNull();
        assertThat(template.headers().get("X-User-Email")).isNull();
    }

    @Test
    void testApply_WithNoRequestContext() {
        // Given
        RequestContextHolder.resetRequestAttributes();

        // When
        interceptor.apply(template);

        // Then
        assertThat(template.headers().get("X-User-Id")).isNull();
        assertThat(template.headers().get("X-User-Role")).isNull();
        assertThat(template.headers().get("X-User-Email")).isNull();
    }

    @Test
    void testApply_WithPartialHeaders() {
        // Given
        ServletRequestAttributes attributes = new ServletRequestAttributes(request);
        RequestContextHolder.setRequestAttributes(attributes);
        
        when(request.getHeader("X-User-Id")).thenReturn("100");
        when(request.getHeader("X-User-Role")).thenReturn(null);
        when(request.getHeader("X-User-Email")).thenReturn("test@example.com");

        // When
        interceptor.apply(template);

        // Then
        Collection<String> userIdHeaders = template.headers().get("X-User-Id");
        Collection<String> userEmailHeaders = template.headers().get("X-User-Email");
        
        assertThat(userIdHeaders).containsExactly("100");
        assertThat(userEmailHeaders).containsExactly("test@example.com");
        assertThat(template.headers().get("X-User-Role")).isNull();
    }
}
