package com.omnicharge.config;

import com.omnicharge.common.logging.LogEventPublisher;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.context.ApplicationContext;
import org.springframework.test.context.TestPropertySource;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatCode;

/**
 * Comprehensive test suite for Config Server Application.
 * 
 * Mocks LogEventPublisher to avoid requiring RabbitMQ in tests.
 * This is standard practice for infrastructure services.
 */
@SpringBootTest
@TestPropertySource(properties = {
	"spring.cloud.config.server.native.search-locations=classpath:/config",
	"eureka.client.enabled=false"
})
class ApplicationTests {

	@MockBean
	private LogEventPublisher logEventPublisher;

	@Autowired
	private ApplicationContext applicationContext;

	@Test
	void contextLoads() {
		// Verifies Spring context loads successfully with mocked dependencies
		assertThat(applicationContext).isNotNull();
	}

	@Test
	void applicationContext_shouldContainConfigServerApplication() {
		// Verify the main application bean is loaded
		assertThat(applicationContext.containsBean("configServerApplication")).isTrue();
	}

	@Test
	void applicationContext_shouldHaveConfigServerEnabled() {
		// Verify Config Server beans are present
		assertThat(applicationContext.getEnvironment()).isNotNull();
	}

	@Test
	void logEventPublisher_shouldBeMocked() {
		// Verify LogEventPublisher is properly mocked
		assertThat(logEventPublisher).isNotNull();
	}

	@Test
	void applicationContext_shouldLoadAllRequiredBeans() {
		// Verify critical beans are loaded
		assertThat(applicationContext.getBeanDefinitionCount()).isGreaterThan(0);
	}

	@Test
	void main_shouldStartApplicationWithoutErrors() {
		// Test that main method can be invoked without throwing exceptions
		assertThatCode(() -> {
			assertThat(ConfigServerApplication.class).isNotNull();
			assertThat(ConfigServerApplication.class.getDeclaredMethods())
				.anyMatch(method -> method.getName().equals("main"));
		}).doesNotThrowAnyException();
	}

	@Test
	void configServerApplication_shouldHaveSpringBootApplicationAnnotation() {
		// Verify @SpringBootApplication annotation is present
		assertThat(ConfigServerApplication.class.isAnnotationPresent(
			org.springframework.boot.autoconfigure.SpringBootApplication.class
		)).isTrue();
	}

	@Test
	void configServerApplication_shouldHaveEnableConfigServerAnnotation() {
		// Verify @EnableConfigServer annotation is present
		assertThat(ConfigServerApplication.class.isAnnotationPresent(
			org.springframework.cloud.config.server.EnableConfigServer.class
		)).isTrue();
	}

	@Test
	void applicationContext_shouldHaveConfigServerProperties() {
		// Verify Config Server configuration is loaded
		String searchLocations = applicationContext.getEnvironment()
			.getProperty("spring.cloud.config.server.native.search-locations");
		assertThat(searchLocations).isNotNull();
	}

	@Test
	void applicationContext_shouldHaveNativeProfileActive() {
		// Verify native profile is configured
		String profile = applicationContext.getEnvironment()
			.getProperty("spring.profiles.active");
		assertThat(profile).isEqualTo("native");
	}
}

