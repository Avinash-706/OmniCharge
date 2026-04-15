package com.omnicharge.operator.config;

import org.junit.jupiter.api.Test;
import org.springframework.amqp.core.Binding;
import org.springframework.amqp.core.Queue;
import org.springframework.amqp.core.TopicExchange;
import org.springframework.amqp.rabbit.connection.ConnectionFactory;
import org.springframework.amqp.rabbit.core.RabbitTemplate;
import org.springframework.amqp.support.converter.Jackson2JsonMessageConverter;
import org.springframework.amqp.support.converter.MessageConverter;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.mock;

class RabbitMQConfigTest {

    private final RabbitMQConfig config = new RabbitMQConfig();

    @Test
    void operatorExchange_ShouldReturnTopicExchange() {
        // Act
        TopicExchange exchange = config.operatorExchange();

        // Assert
        assertThat(exchange).isNotNull();
        assertThat(exchange.getName()).isEqualTo("operator.exchange");
        assertThat(exchange.getType()).isEqualTo("topic");
    }

    @Test
    void planUpdateQueue_ShouldReturnQueue() {
        // Act
        Queue queue = config.planUpdateQueue();

        // Assert
        assertThat(queue).isNotNull();
        assertThat(queue.getName()).isEqualTo("operator.plan.updates");
    }

    @Test
    void binding_ShouldBindQueueToExchange() {
        // Arrange
        Queue queue = config.planUpdateQueue();
        TopicExchange exchange = config.operatorExchange();

        // Act
        Binding binding = config.binding(queue, exchange);

        // Assert
        assertThat(binding).isNotNull();
        assertThat(binding.getDestination()).isEqualTo("operator.plan.updates");
        assertThat(binding.getExchange()).isEqualTo("operator.exchange");
        assertThat(binding.getRoutingKey()).isEqualTo("plan.updated");
    }

    @Test
    void jsonMessageConverter_ShouldReturnJackson2JsonMessageConverter() {
        // Act
        MessageConverter converter = config.jsonMessageConverter();

        // Assert
        assertThat(converter).isNotNull();
        assertThat(converter).isInstanceOf(Jackson2JsonMessageConverter.class);
    }

    @Test
    void rabbitTemplate_ShouldConfigureWithJsonConverter() {
        // Arrange
        ConnectionFactory connectionFactory = mock(ConnectionFactory.class);

        // Act
        RabbitTemplate template = config.rabbitTemplate(connectionFactory);

        // Assert
        assertThat(template).isNotNull();
        assertThat(template.getMessageConverter()).isInstanceOf(Jackson2JsonMessageConverter.class);
    }

    @Test
    void constants_ShouldHaveCorrectValues() {
        // Assert
        assertThat(RabbitMQConfig.EXCHANGE).isEqualTo("operator.exchange");
        assertThat(RabbitMQConfig.PLAN_UPDATE_QUEUE).isEqualTo("operator.plan.updates");
    }

    @Test
    void shouldBeConfigurationClass() {
        // Assert
        assertThat(RabbitMQConfig.class.isAnnotationPresent(org.springframework.context.annotation.Configuration.class)).isTrue();
    }
}
