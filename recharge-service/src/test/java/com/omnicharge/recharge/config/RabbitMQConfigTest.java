package com.omnicharge.recharge.config;

import org.junit.jupiter.api.Test;
import org.springframework.amqp.core.Binding;
import org.springframework.amqp.core.Queue;
import org.springframework.amqp.core.TopicExchange;
import org.springframework.amqp.rabbit.connection.ConnectionFactory;
import org.springframework.amqp.rabbit.core.RabbitTemplate;
import org.springframework.amqp.support.converter.MessageConverter;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.mock;

class RabbitMQConfigTest {

    private final RabbitMQConfig config = new RabbitMQConfig();

    @Test
    void testExchangeBean() {
        // When
        TopicExchange exchange = config.exchange();

        // Then
        assertThat(exchange).isNotNull();
        assertThat(exchange.getName()).isEqualTo("omnicharge.exchange");
    }

    @Test
    void testPaymentApprovedQueueBean() {
        // When
        Queue queue = config.paymentApprovedQueue();

        // Then
        assertThat(queue).isNotNull();
        assertThat(queue.getName()).isEqualTo("saga.recharge.approved");
    }

    @Test
    void testPaymentRejectedQueueBean() {
        // When
        Queue queue = config.paymentRejectedQueue();

        // Then
        assertThat(queue).isNotNull();
        assertThat(queue.getName()).isEqualTo("saga.recharge.rejected");
    }

    @Test
    void testPaymentApprovedBindingBean() {
        // Given
        Queue queue = config.paymentApprovedQueue();
        TopicExchange exchange = config.exchange();

        // When
        Binding binding = config.paymentApprovedBinding(queue, exchange);

        // Then
        assertThat(binding).isNotNull();
        assertThat(binding.getDestination()).isEqualTo("saga.recharge.approved");
        assertThat(binding.getRoutingKey()).isEqualTo("saga.payment.approved");
    }

    @Test
    void testPaymentRejectedBindingBean() {
        // Given
        Queue queue = config.paymentRejectedQueue();
        TopicExchange exchange = config.exchange();

        // When
        Binding binding = config.paymentRejectedBinding(queue, exchange);

        // Then
        assertThat(binding).isNotNull();
        assertThat(binding.getDestination()).isEqualTo("saga.recharge.rejected");
        assertThat(binding.getRoutingKey()).isEqualTo("saga.payment.rejected");
    }

    @Test
    void testJsonMessageConverterBean() {
        // When
        MessageConverter converter = config.jsonMessageConverter();

        // Then
        assertThat(converter).isNotNull();
    }

    @Test
    void testRabbitTemplateBean() {
        // Given
        ConnectionFactory connectionFactory = mock(ConnectionFactory.class);

        // When
        RabbitTemplate template = config.rabbitTemplate(connectionFactory);

        // Then
        assertThat(template).isNotNull();
        assertThat(template.getMessageConverter()).isNotNull();
    }
}
