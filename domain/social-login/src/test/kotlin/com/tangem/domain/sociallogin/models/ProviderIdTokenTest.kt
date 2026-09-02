package com.tangem.domain.sociallogin.models

import com.google.common.truth.Truth.assertThat
import org.junit.jupiter.api.Test

internal class ProviderIdTokenTest {

    @Test
    fun `GIVEN token WHEN printed THEN value is masked`() {
        // Arrange
        val token = ProviderIdToken(value = "eyJhbGciOiJSUzI1NiJ9.payload.signature")

        // Assert
        assertThat(token.toString()).isEqualTo("ProviderIdToken(***)")
    }
}