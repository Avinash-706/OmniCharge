package com.omnicharge.user.config;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.amqp.core.TopicExchange;
import org.springframework.amqp.rabbit.connection.ConnectionFactory;
import org.springframework.amqp.rabbit.core.RabbitTemplate;
import org.springframework.amqp.support.converter.Jackson2JsonMessageConverter;
import org.springframework.amqp.support.converter.MessageConverter;

import static org.assertj.core.api.Assertions.assertThat;

@ExtendWith(MockitoExtension.class)
class RabbitMQConfigTest {

    @Mock
    private ConnectionFactory connectionFactory;

    @InjectMocks
    private RabbitMQConfig rabbitMQConfig;

    @Test
    void exchange_ShouldReturnTopicExchange() {
        // When
        TopicExchange exchange = rabbitMQConfig.exchange();

        // Then
        assertThat(exchange).isNotNull();
        assertThat(exchange.getName()).isEqualTo("omnicharge.exchange");
    }

    @Test
    void jsonMessageConverter_ShouldReturnJackson2JsonMessageConverter() {
        // When
        MessageConverter converter = rabbitMQConfig.jsonMessageConverter();

        // Then
        assertThat(converter).isNotNull();
        assertThat(converter).isInstanceOf(Jackson2JsonMessageConverter.class);
    }

    @Test
    void rabbitTemplate_ShouldReturnConfiguredRabbitTemplate() {
        // When
        RabbitTemplate template = rabbitMQConfig.rabbitTemplate(connectionFactory);

        // Then
        assertThat(template).isNotNull();
        assertThat(template.getConnectionFactory()).isEqualTo(connectionFactory);
        assertThat(template.getMessageConverter()).isInstanceOf(Jackson2JsonMessageConverter.class);
    }
}
