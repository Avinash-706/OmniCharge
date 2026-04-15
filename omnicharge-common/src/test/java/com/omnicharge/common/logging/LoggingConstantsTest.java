package com.omnicharge.common.logging;

import org.junit.jupiter.api.Test;
import static org.assertj.core.api.Assertions.assertThat;

class LoggingConstantsTest {
    @Test
    void constants_shouldHaveCorrectValues() {
        assertThat(LoggingConstants.LOGGING_EXCHANGE).isEqualTo("omnicharge.logging.exchange");
        assertThat(LoggingConstants.LOGGING_QUEUE).isEqualTo("logging.events.queue");
        assertThat(LoggingConstants.LOGGING_ROUTING_KEY).isEqualTo("log.#");
    }
}
