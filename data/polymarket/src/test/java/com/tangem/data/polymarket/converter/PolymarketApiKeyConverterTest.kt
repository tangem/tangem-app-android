package com.tangem.data.polymarket.converter

import com.google.common.truth.Truth.assertThat
import com.tangem.datasource.api.polymarket.clob.models.PolymarketApiKeyResponse
import com.tangem.domain.polymarket.model.PolymarketApiCredentials
import org.junit.jupiter.api.Test

internal class PolymarketApiKeyConverterTest {

    @Test
    fun `GIVEN api key response WHEN convert THEN maps every field`() {
        // Arrange
        val response = PolymarketApiKeyResponse(apiKey = "k", secret = "s", passphrase = "p")

        // Act
        val actual = PolymarketApiKeyConverter.convert(response)

        // Assert
        assertThat(actual).isEqualTo(PolymarketApiCredentials(apiKey = "k", secret = "s", passphrase = "p"))
    }
}