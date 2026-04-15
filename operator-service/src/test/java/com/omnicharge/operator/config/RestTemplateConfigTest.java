package com.omnicharge.operator.config;

import org.junit.jupiter.api.Test;
import org.springframework.web.client.RestTemplate;

import static org.assertj.core.api.Assertions.assertThat;

class RestTemplateConfigTest {

    private final RestTemplateConfig config = new RestTemplateConfig();

    @Test
    void restTemplate_ShouldReturnNewInstance() {
        // Act
        RestTemplate restTemplate = config.restTemplate();

        // Assert
        assertThat(restTemplate).isNotNull();
    }

    @Test
    void restTemplate_ShouldReturnDifferentInstances() {
        // Act
        RestTemplate restTemplate1 = config.restTemplate();
        RestTemplate restTemplate2 = config.restTemplate();

        // Assert
        assertThat(restTemplate1).isNotNull();
        assertThat(restTemplate2).isNotNull();
        assertThat(restTemplate1).isNotSameAs(restTemplate2);
    }

    @Test
    void restTemplate_ShouldHaveDefaultConfiguration() {
        // Act
        RestTemplate restTemplate = config.restTemplate();

        // Assert
        assertThat(restTemplate.getMessageConverters()).isNotEmpty();
        assertThat(restTemplate.getInterceptors()).isEmpty();
    }

    @Test
    void shouldBeConfigurationClass() {
        // Assert
        assertThat(RestTemplateConfig.class.isAnnotationPresent(org.springframework.context.annotation.Configuration.class)).isTrue();
    }

    @Test
    void restTemplate_ShouldBeBeanAnnotated() throws NoSuchMethodException {
        // Assert
        assertThat(RestTemplateConfig.class.getMethod("restTemplate")
            .isAnnotationPresent(org.springframework.context.annotation.Bean.class)).isTrue();
    }
}
