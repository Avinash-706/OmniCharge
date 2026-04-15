package com.omnicharge.common.logging;

import jakarta.servlet.FilterChain;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class LogCaptureFilterTest {
    @Mock
    private LogEventPublisher logEventPublisher;
    @Mock
    private HttpServletRequest request;
    @Mock
    private HttpServletResponse response;
    @Mock
    private FilterChain filterChain;
    private LogCaptureFilter filter;

    @BeforeEach
    void setUp() {
        filter = new LogCaptureFilter(logEventPublisher);
    }

    @Test
    void doFilterInternal_shouldLogHttpRequest() throws Exception {
        when(request.getMethod()).thenReturn("GET");
        when(request.getRequestURI()).thenReturn("/api/users");
        when(response.getStatus()).thenReturn(200);
        ArgumentCaptor<LogEvent> captor = ArgumentCaptor.forClass(LogEvent.class);
        
        filter.doFilterInternal(request, response, filterChain);
        
        verify(filterChain).doFilter(request, response);
        verify(logEventPublisher).publish(captor.capture());
        LogEvent event = captor.getValue();
        assertThat(event.getMessage()).contains("GET", "/api/users", "200");
    }

    @Test
    void shouldNotFilter_shouldSkipActuatorEndpoints() throws Exception {
        when(request.getRequestURI()).thenReturn("/actuator/health");
        boolean result = filter.shouldNotFilter(request);
        assertThat(result).isTrue();
    }
}
