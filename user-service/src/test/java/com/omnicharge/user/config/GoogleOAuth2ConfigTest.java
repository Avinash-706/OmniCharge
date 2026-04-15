package com.omnicharge.user.config;

import com.google.api.client.googleapis.auth.oauth2.GoogleIdTokenVerifier;
import org.junit.jupiter.api.Test;
import org.springframework.test.util.ReflectionTestUtils;

import static org.assertj.core.api.Assertions.assertThat;

class GoogleOAuth2ConfigTest {

    @Test
    void googleIdTokenVerifier_ShouldReturnConfiguredVerifier() {
        // Given
        GoogleOAuth2Config config = new GoogleOAuth2Config();
        ReflectionTestUtils.setField(config, "googleClientId", "test-client-id");

        // When
        GoogleIdTokenVerifier verifier = config.googleIdTokenVerifier();

        // Then
        assertThat(verifier).isNotNull();
    }
}
