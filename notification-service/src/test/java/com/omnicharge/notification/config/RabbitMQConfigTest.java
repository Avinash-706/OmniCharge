package com.omnicharge.notification.config;

import org.junit.jupiter.api.Test;
import org.springframework.amqp.core.*;
import org.springframework.amqp.rabbit.connection.ConnectionFactory;
import org.springframework.amqp.rabbit.core.RabbitTemplate;
import org.springframework.amqp.support.converter.MessageConverter;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.mock;

class RabbitMQConfigTest {

    private final RabbitMQConfig config = new RabbitMQConfig();

    @Test
    void exchange_ReturnsTopicExchange() {
        TopicExchange exchange = config.exchange();
        
        assertNotNull(exchange);
        assertEquals("omnicharge.exchange", exchange.getName());
    }

    @Test
    void rechargeQueue_ReturnsQueue() {
        Queue queue = config.rechargeQueue();
        
        assertNotNull(queue);
        assertEquals("notification.recharge.queue", queue.getName());
        assertTrue(queue.isDurable());
    }

    @Test
    void paymentQueue_ReturnsQueue() {
        Queue queue = config.paymentQueue();
        
        assertNotNull(queue);
        assertEquals("notification.payment.queue", queue.getName());
        assertTrue(queue.isDurable());
    }

    @Test
    void planExpiryQueue_ReturnsQueue() {
        Queue queue = config.planExpiryQueue();
        
        assertNotNull(queue);
        assertEquals("notification.plan.expiry.queue", queue.getName());
        assertTrue(queue.isDurable());
    }

    @Test
    void otpQueue_ReturnsQueue() {
        Queue queue = config.otpQueue();
        
        assertNotNull(queue);
        assertEquals("notification.otp.queue", queue.getName());
        assertTrue(queue.isDurable());
    }

    @Test
    void rechargeBinding_BindsQueueToExchange() {
        Queue queue = config.rechargeQueue();
        TopicExchange exchange = config.exchange();
        
        Binding binding = config.rechargeBinding(queue, exchange);
        
        assertNotNull(binding);
        assertEquals("notification.recharge.queue", binding.getDestination());
        assertEquals("recharge.completed", binding.getRoutingKey());
    }

    @Test
    void paymentBinding_BindsQueueToExchange() {
        Queue queue = config.paymentQueue();
        TopicExchange exchange = config.exchange();
        
        Binding binding = config.paymentBinding(queue, exchange);
        
        assertNotNull(binding);
        assertEquals("notification.payment.queue", binding.getDestination());
        assertEquals("payment.completed", binding.getRoutingKey());
    }

    @Test
    void planExpiryBinding_BindsQueueToExchange() {
        Queue queue = config.planExpiryQueue();
        TopicExchange exchange = config.exchange();
        
        Binding binding = config.planExpiryBinding(queue, exchange);
        
        assertNotNull(binding);
        assertEquals("notification.plan.expiry.queue", binding.getDestination());
        assertEquals("plan.expiry", binding.getRoutingKey());
    }

    @Test
    void otpBinding_BindsQueueToExchange() {
        Queue queue = config.otpQueue();
        TopicExchange exchange = config.exchange();
        
        Binding binding = config.otpBinding(queue, exchange);
        
        assertNotNull(binding);
        assertEquals("notification.otp.queue", binding.getDestination());
        assertEquals("mobile.otp.send", binding.getRoutingKey());
    }

    @Test
    void jsonMessageConverter_ReturnsConverter() {
        MessageConverter converter = config.jsonMessageConverter();
        
        assertNotNull(converter);
        assertTrue(converter instanceof org.springframework.amqp.support.converter.Jackson2JsonMessageConverter);
    }

    @Test
    void rabbitTemplate_ConfiguresTemplate() {
        ConnectionFactory connectionFactory = mock(ConnectionFactory.class);
        
        RabbitTemplate template = config.rabbitTemplate(connectionFactory);
        
        assertNotNull(template);
        assertNotNull(template.getMessageConverter());
    }
}
