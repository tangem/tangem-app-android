package com.tangem.data.polymarket.converter

import com.google.common.truth.Truth.assertThat
import com.tangem.data.polymarket.entity.PolymarketApiCredentialsDTO
import com.tangem.domain.polymarket.model.PolymarketApiCredentials
import org.junit.jupiter.api.Test

internal class PolymarketApiCredentialsConverterTest {

    @Test
    fun `GIVEN domain credentials WHEN convert THEN maps every field to the dto`() {
        // Act
        val actual = PolymarketApiCredentialsConverter.convert(CREDENTIALS)

        // Assert
        assertThat(actual).isEqualTo(DTO)
    }

    @Test
    fun `GIVEN dto WHEN convertBack THEN maps every field to the domain model`() {
        // Act
        val actual = PolymarketApiCredentialsConverter.convertBack(DTO)

        // Assert
        assertThat(actual).isEqualTo(CREDENTIALS)
    }

    @Test
    fun `GIVEN domain credentials WHEN round-tripped THEN unchanged`() {
        // Act
        val actual = PolymarketApiCredentialsConverter.convertBack(
            PolymarketApiCredentialsConverter.convert(CREDENTIALS),
        )

        // Assert
        assertThat(actual).isEqualTo(CREDENTIALS)
    }

    private companion object {
        val CREDENTIALS = PolymarketApiCredentials(
            apiKey = "df2b7b32-a2e6-4a3f-9b1c-0f0e5f5f0000",
            secret = "c2VjcmV0LWJ5dGVzLWJhc2U2NA==",
            passphrase = "passphrase-value",
        )

        val DTO = PolymarketApiCredentialsDTO(
            apiKey = "df2b7b32-a2e6-4a3f-9b1c-0f0e5f5f0000",
            secret = "c2VjcmV0LWJ5dGVzLWJhc2U2NA==",
            passphrase = "passphrase-value",
        )
    }
}