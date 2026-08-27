package com.tangem.domain.polymarket.model

import com.google.common.truth.Truth.assertThat
import org.junit.jupiter.api.Test

internal class PolymarketApiCredentialsTest {

    @Test
    fun `GIVEN credentials WHEN toString THEN no credential value is exposed`() {
        // Arrange
        val credentials = PolymarketApiCredentials(
            apiKey = "df2b7b32-a2e6-4a3f-9b1c-0f0e5f5f0000",
            secret = "c2VjcmV0LWJ5dGVzLWJhc2U2NA==",
            passphrase = "passphrase-value",
        )

        // Act
        val actual = credentials.toString()

        // Assert
        assertThat(actual).doesNotContain("df2b7b32-a2e6-4a3f-9b1c-0f0e5f5f0000")
        assertThat(actual).doesNotContain("c2VjcmV0LWJ5dGVzLWJhc2U2NA==")
        assertThat(actual).doesNotContain("passphrase-value")
    }

    @Test
    fun `GIVEN equal credentials WHEN compared THEN equality is unaffected by the toString override`() {
        // Arrange
        val credentials = PolymarketApiCredentials(apiKey = "k", secret = "s", passphrase = "p")

        // Act & Assert
        assertThat(credentials).isEqualTo(PolymarketApiCredentials(apiKey = "k", secret = "s", passphrase = "p"))
    }
}