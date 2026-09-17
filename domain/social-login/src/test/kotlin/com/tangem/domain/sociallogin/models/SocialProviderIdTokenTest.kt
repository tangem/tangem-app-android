package com.tangem.domain.sociallogin.models

import com.google.common.truth.Truth.assertThat
import org.junit.jupiter.api.Test

internal class SocialProviderIdTokenTest {

    @Test
    fun `GIVEN token WHEN printed THEN value is masked`() {
        // Arrange
        val token = SocialProviderIdToken(value = "eyJhbGciOiJSUzI1NiJ9.payload.signature", nonce = "nonce")

        // Assert
        assertThat(token.toString()).isEqualTo("SocialProviderIdToken(***)")
    }
}