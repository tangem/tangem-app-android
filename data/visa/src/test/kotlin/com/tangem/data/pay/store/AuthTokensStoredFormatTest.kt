package com.tangem.data.pay.store

import com.google.common.truth.Truth.assertThat
import com.tangem.core.remote.json.NetworkJsonFactory
import com.tangem.domain.visa.model.TangemPayAuthTokens
import com.tangem.domain.visa.model.VisaAuthTokens
import com.tangem.test.core.ProvideTestModels
import kotlinx.serialization.json.Json
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.TestInstance
import org.junit.jupiter.params.ParameterizedTest

@TestInstance(TestInstance.Lifecycle.PER_CLASS)
internal class AuthTokensStoredFormatTest {

    @ParameterizedTest
    @ProvideTestModels
    fun `GIVEN Tangem Pay tokens stored by the previous app version WHEN decoded THEN they are read back`(
        model: TangemPayTokensModel,
    ) {
        // Act
        val actual = NetworkJsonFactory.create().decodeFromString(TangemPayAuthTokens.serializer(), model.json)

        // Assert
        assertThat(actual).isEqualTo(model.expected)
    }

    @Test
    fun `GIVEN Visa card tokens stored by the previous app version WHEN decoded THEN they are read back`() {
        // Arrange
        val stored = """{"accessToken":"access","refreshToken":{"value":"refresh","authType":"CardWallet"}}"""

        // Act
        val actual = Json { ignoreUnknownKeys = true }.decodeFromString(VisaAuthTokens.serializer(), stored)

        // Assert
        assertThat(actual).isEqualTo(
            VisaAuthTokens(
                accessToken = "access",
                refreshToken = VisaAuthTokens.RefreshToken(
                    value = "refresh",
                    authType = VisaAuthTokens.RefreshToken.Type.CardWallet,
                ),
            ),
        )
    }

    private fun provideTestModels() = listOf(
        TangemPayTokensModel(
            name = "with idempotency key",
            json = """{$TANGEM_PAY_TOKENS_FIELDS,"idempotency_key":"key"}""",
            expected = TANGEM_PAY_TOKENS.copy(idempotencyKey = "key"),
        ),
        TangemPayTokensModel(
            name = "with null idempotency key",
            json = """{$TANGEM_PAY_TOKENS_FIELDS,"idempotency_key":null}""",
            expected = TANGEM_PAY_TOKENS,
        ),
        TangemPayTokensModel(
            name = "without idempotency key",
            json = """{$TANGEM_PAY_TOKENS_FIELDS}""",
            expected = TANGEM_PAY_TOKENS,
        ),
    )

    internal data class TangemPayTokensModel(val name: String, val json: String, val expected: TangemPayAuthTokens) {
        override fun toString(): String = name
    }

    private companion object {
        const val TANGEM_PAY_TOKENS_FIELDS = """"access_token":"access","expires_at":1700000000,""" +
            """"refresh_token":"refresh","refresh_expires_at":1700003600"""
        val TANGEM_PAY_TOKENS = TangemPayAuthTokens(
            accessToken = "access",
            expiresAt = 1_700_000_000L,
            refreshToken = "refresh",
            refreshExpiresAt = 1_700_003_600L,
            idempotencyKey = null,
        )
    }
}