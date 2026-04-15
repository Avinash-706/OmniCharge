package com.omnicharge.common.logging;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.amqp.rabbit.core.RabbitTemplate;
import org.springframework.test.util.ReflectionTestUtils;
import java.time.LocalDateTime;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class LogEventPublisherTest {
    @Mock
    private RabbitTemplate rabbitTemplate;
    @Mock
    private FallbackLogWriter fallbackLogWriter;
    private LogEventPublisher publisher;

    @BeforeEach
    void setUp() {
        publisher = new LogEventPublisher(rabbitTemplate, fallbackLogWriter);
        ReflectionTestUtils.setField(publisher, "serviceName", "test-service");
    }

    @Test
    void publish_shouldSendToRabbitMQ() {
        LogEvent event = LogEvent.builder().level("INFO").message("Test").timestamp(LocalDateTime.now()).build();
        
        publisher.publish(event);
        
        verify(rabbitTemplate).convertAndSend(eq(LoggingConstants.LOGGING_EXCHANGE), eq("log.test-service"), eq(event));
        verify(fallbackLogWriter, never()).writeToFallbackFile(any());
    }

    @Test
    void publish_shouldFallbackToFileWhenRabbitMQFails() {
        LogEvent event = LogEvent.builder().level("INFO").message("Test").timestamp(LocalDateTime.now()).build();
        doThrow(new RuntimeException("Connection failed")).when(rabbitTemplate)
                .convertAndSend(eq(LoggingConstants.LOGGING_EXCHANGE), eq("log.test-service"), eq(event));
        
        publisher.publish(event);
        
        verify(fallbackLogWriter).writeToFallbackFile(event);
    }
}
