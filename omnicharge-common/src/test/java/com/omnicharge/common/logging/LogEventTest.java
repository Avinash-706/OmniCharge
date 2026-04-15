package com.omnicharge.common.logging;

import org.junit.jupiter.api.Test;
import java.io.Serializable;
import java.time.LocalDateTime;
import java.util.HashMap;
import java.util.Map;
import static org.assertj.core.api.Assertions.assertThat;

class LogEventTest {
    @Test
    void logEvent_shouldImplementSerializable() {
        assertThat(Serializable.class).isAssignableFrom(LogEvent.class);
    }

    @Test
    void builder_shouldCreateLogEventWithAllFields() {
        Map<String, Object> context = new HashMap<>();
        context.put("key", "value");
        LocalDateTime now = LocalDateTime.now();
        
        LogEvent event = LogEvent.builder()
                .serviceName("test-service").level("INFO").logger("TestLogger")
                .message("Test message").traceId("trace123").spanId("span456")
                .threadName("main").stackTrace(null).timestamp(now)
                .eventType("TEST").context(context).build();
        
        assertThat(event.getServiceName()).isEqualTo("test-service");
        assertThat(event.getLevel()).isEqualTo("INFO");
        assertThat(event.getMessage()).isEqualTo("Test message");
        assertThat(event.getEventType()).isEqualTo("TEST");
        assertThat(event.getContext()).containsKey("key");
    }
}
