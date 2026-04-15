package com.omnicharge.payment.config;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.amqp.core.Binding;
import org.springframework.amqp.core.Queue;
import org.springframework.amqp.core.TopicExchange;
import org.springframework.amqp.rabbit.connection.ConnectionFactory;
import org.springframework.amqp.rabbit.core.RabbitTemplate;
import org.springframework.amqp.support.converter.Jackson2JsonMessageConverter;
import org.springframework.amqp.support.converter.MessageConverter;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class RabbitMQConfigTest {

    private RabbitMQConfig config;

    @Mock
    private ConnectionFactory connectionFactory;

    @BeforeEach
    void setUp() {
        config = new RabbitMQConfig();
    }

    @Test
    void testExchangeBean() {
        TopicExchange exchange = config.exchange();
        
        assertNotNull(exchange);
        assertEquals("omnicharge.exchange", exchange.getName());
    }

    @Test
    void testPaymentProcessQueueBean() {
        Queue queue = config.paymentProcessQueue();
        
        assertNotNull(queue);
        assertEquals("saga.payment.process", queue.getName());
    }

    @Test
    void testPaymentProcessBindingBean() {
        Queue queue = config.paymentProcessQueue();
        TopicExchange exchange = config.exchange();
        
        Binding binding = config.paymentProcessBinding(queue, exchange);
        
        assertNotNull(binding);
        assertEquals("saga.payment.process", binding.getDestination());
        assertEquals("saga.recharge.initiated", binding.getRoutingKey());
    }

    @Test
    void testJsonMessageConverterBean() {
        MessageConverter converter = config.jsonMessageConverter();
        
        assertNotNull(converter);
        assertTrue(converter instanceof Jackson2JsonMessageConverter);
    }

    @Test
    void testRabbitTemplateBean() {
        RabbitTemplate template = config.rabbitTemplate(connectionFactory);
        
        assertNotNull(template);
        assertNotNull(template.getMessageConverter());
        assertTrue(template.getMessageConverter() instanceof Jackson2JsonMessageConverter);
    }

    @Test
    void testRabbitTemplateUsesConnectionFactory() {
        RabbitTemplate template = config.rabbitTemplate(connectionFactory);
        
        assertNotNull(template);
        verify(connectionFactory, never()).createConnection();
    }

    @Test
    void testExchangeIsDurable() {
        TopicExchange exchange = config.exchange();
        
        assertTrue(exchange.isDurable());
    }

    @Test
    void testQueueIsDurable() {
        Queue queue = config.paymentProcessQueue();
        
        assertTrue(queue.isDurable());
    }

    @Test
    void testBindingRoutingKey() {
        Queue queue = config.paymentProcessQueue();
        TopicExchange exchange = config.exchange();
        Binding binding = config.paymentProcessBinding(queue, exchange);
        
        assertEquals("saga.recharge.initiated", binding.getRoutingKey());
    }

    @Test
    void testMultipleExchangeCallsReturnNewInstances() {
        TopicExchange exchange1 = config.exchange();
        TopicExchange exchange2 = config.exchange();
        
        assertNotSame(exchange1, exchange2);
        assertEquals(exchange1.getName(), exchange2.getName());
    }
}
