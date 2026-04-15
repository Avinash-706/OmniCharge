package com.omnicharge.common.logging;

import org.junit.jupiter.api.Test;
import org.springframework.amqp.support.converter.Jackson2JsonMessageConverter;
import org.springframework.amqp.support.converter.MessageConverter;
import static org.assertj.core.api.Assertions.assertThat;

class CommonRabbitMQConfigTest {
    @Test
    void commonJsonMessageConverter_shouldReturnJackson2Converter() {
        CommonRabbitMQConfig config = new CommonRabbitMQConfig();
        MessageConverter converter = config.commonJsonMessageConverter();
        assertThat(converter).isInstanceOf(Jackson2JsonMessageConverter.class);
    }
}
