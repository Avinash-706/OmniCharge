package com.omnicharge.common.security;

import org.junit.jupiter.api.Test;
import static org.assertj.core.api.Assertions.assertThat;

class JwtConstantsTest {
    @Test
    void constants_shouldHaveCorrectValues() {
        assertThat(JwtConstants.JWT_HEADER).isEqualTo("Authorization");
        assertThat(JwtConstants.JWT_TOKEN_PREFIX).isEqualTo("Bearer ");
        assertThat(JwtConstants.JWT_CLAIM_USER_ID).isEqualTo("userId");
        assertThat(JwtConstants.JWT_CLAIM_ROLE).isEqualTo("role");
        assertThat(JwtConstants.JWT_CLAIM_EMAIL).isEqualTo("email");
    }
}
