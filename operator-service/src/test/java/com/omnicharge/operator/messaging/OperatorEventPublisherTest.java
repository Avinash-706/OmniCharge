package com.omnicharge.operator.messaging;

import com.omnicharge.operator.event.PlanUpdatedMessage;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.amqp.rabbit.core.RabbitTemplate;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class OperatorEventPublisherTest {

    @Mock
    private RabbitTemplate rabbitTemplate;

    @InjectMocks
    private OperatorEventPublisher operatorEventPublisher;

    @BeforeEach
    void setUp() {
        // Reset mocks before each test
    }

    @Test
    void testPublishPlanUpdatedEvent_Success() {
        // Arrange
        Long operatorId = 1L;
        
        // Act
        operatorEventPublisher.publishPlanUpdatedEvent(operatorId);
        
        // Assert
        ArgumentCaptor<PlanUpdatedMessage> messageCaptor = ArgumentCaptor.forClass(PlanUpdatedMessage.class);
        verify(rabbitTemplate, times(1)).convertAndSend(
                eq("operator.exchange"),
                eq("plan.updated"),
                messageCaptor.capture()
        );
        
        PlanUpdatedMessage capturedMessage = messageCaptor.getValue();
        assertThat(capturedMessage.getOperatorId()).isEqualTo(operatorId);
        assertThat(capturedMessage.getEventId()).isNotNull();
        assertThat(capturedMessage.getTimestamp()).isNotNull();
    }

    @Test
    void testPublishPlanUpdatedEvent_MultipleOperators() {
        // Arrange & Act
        operatorEventPublisher.publishPlanUpdatedEvent(1L);
        operatorEventPublisher.publishPlanUpdatedEvent(2L);
        operatorEventPublisher.publishPlanUpdatedEvent(3L);
        
        // Assert
        verify(rabbitTemplate, times(3)).convertAndSend(
                eq("operator.exchange"),
                eq("plan.updated"),
                any(PlanUpdatedMessage.class)
        );
    }

    @Test
    void testPublishPlanUpdatedEvent_UniqueEventIds() {
        // Arrange
        ArgumentCaptor<PlanUpdatedMessage> messageCaptor = ArgumentCaptor.forClass(PlanUpdatedMessage.class);
        
        // Act
        operatorEventPublisher.publishPlanUpdatedEvent(1L);
        operatorEventPublisher.publishPlanUpdatedEvent(1L);
        
        // Assert
        verify(rabbitTemplate, times(2)).convertAndSend(
                eq("operator.exchange"),
                eq("plan.updated"),
                messageCaptor.capture()
        );
        
        String firstEventId = messageCaptor.getAllValues().get(0).getEventId();
        String secondEventId = messageCaptor.getAllValues().get(1).getEventId();
        
        assertThat(firstEventId).isNotEqualTo(secondEventId);
    }

    @Test
    void testPublishPlanUpdatedEvent_RabbitTemplateException() {
        // Arrange
        Long operatorId = 1L;
        doThrow(new RuntimeException("RabbitMQ connection error"))
                .when(rabbitTemplate).convertAndSend(anyString(), anyString(), any(PlanUpdatedMessage.class));
        
        // Act & Assert - should not throw exception, just log error
        operatorEventPublisher.publishPlanUpdatedEvent(operatorId);
        
        verify(rabbitTemplate, times(1)).convertAndSend(anyString(), anyString(), any(PlanUpdatedMessage.class));
    }

    @Test
    void testPublishPlanUpdatedEvent_NullOperatorId() {
        // Arrange & Act
        operatorEventPublisher.publishPlanUpdatedEvent(null);
        
        // Assert
        ArgumentCaptor<PlanUpdatedMessage> messageCaptor = ArgumentCaptor.forClass(PlanUpdatedMessage.class);
        verify(rabbitTemplate, times(1)).convertAndSend(
                eq("operator.exchange"),
                eq("plan.updated"),
                messageCaptor.capture()
        );
        
        assertThat(messageCaptor.getValue().getOperatorId()).isNull();
    }

    @Test
    void testPublishPlanUpdatedEvent_VerifyExchangeAndRoutingKey() {
        // Arrange & Act
        operatorEventPublisher.publishPlanUpdatedEvent(1L);
        
        // Assert - verify correct exchange and routing key
        verify(rabbitTemplate, times(1)).convertAndSend(
                eq("operator.exchange"),
                eq("plan.updated"),
                any(PlanUpdatedMessage.class)
        );
    }

    @Test
    void testPublishPlanUpdatedEvent_MessageStructure() {
        // Arrange
        Long operatorId = 5L;
        ArgumentCaptor<PlanUpdatedMessage> messageCaptor = ArgumentCaptor.forClass(PlanUpdatedMessage.class);
        
        // Act
        operatorEventPublisher.publishPlanUpdatedEvent(operatorId);
        
        // Assert
        verify(rabbitTemplate, times(1)).convertAndSend(
                eq("operator.exchange"),
                eq("plan.updated"),
                messageCaptor.capture()
        );
        
        PlanUpdatedMessage message = messageCaptor.getValue();
        assertThat(message.getOperatorId()).isEqualTo(operatorId);
        assertThat(message.getEventId()).isNotNull().isNotEmpty();
        assertThat(message.getTimestamp()).isNotNull().isPositive();
    }
}
