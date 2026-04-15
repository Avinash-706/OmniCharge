package com.omnicharge.common.security;

import org.junit.jupiter.api.Test;
import static org.assertj.core.api.Assertions.assertThat;

class SecurityConstantsTest {
    @Test
    void constants_shouldHaveCorrectValues() {
        assertThat(SecurityConstants.ROLE_USER).isEqualTo("ROLE_USER");
        assertThat(SecurityConstants.ROLE_ADMIN).isEqualTo("ROLE_ADMIN");
        assertThat(SecurityConstants.PUBLIC_PATHS).isNotEmpty();
        assertThat(SecurityConstants.PUBLIC_PATHS).contains("/api/auth/login", "/api/auth/register");
    }
}
