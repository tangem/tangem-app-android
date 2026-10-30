package com.tangem.grow.datasource.express

import com.google.common.truth.Truth.assertThat
import com.squareup.moshi.JsonDataException
import com.squareup.moshi.Moshi
import com.tangem.grow.datasource.express.models.response.ExchangeIntent
import com.tangem.grow.datasource.express.models.response.ExchangeIntentResponse
import com.tangem.grow.datasource.express.models.response.ExchangeIntentTransaction
import com.tangem.grow.datasource.express.models.response.RateType
import org.junit.jupiter.api.Test

internal class ExchangeIntentResponseTest {

    private val adapter = Moshi.Builder().build().adapter(ExchangeIntentResponse::class.java)

    @Test
    fun `GIVEN intent with no transactions WHEN parsed THEN intent and empty transactions are read`() {
        // Arrange
        val json = """
            {
              "intent": {
                "clientId": "claude-code",
                "intentId": "fb9488fc-e44c-435c-b074-144b5a288c42",
                "provider": "changelly",
                "rateType": "float",
                "fromContractAddress": "0",
                "fromNetwork": "ethereum",
                "fromDecimals": 18,
                "fromAmount": "1000000000000000000",
                "toContractAddress": "0",
                "toNetwork": "tron",
                "toDecimals": 6,
                "fromAddress": "0xFromWalletAddress",
                "payoutAddress": "TToWalletAddress",
                "sendNotification": true,
                "sessionId": "session-1",
                "walletId": "wallet-1"
              },
              "transactions": []
            }
        """.trimIndent()

        // Act
        val actual = adapter.fromJson(json)

        // Assert
        val expected = ExchangeIntentResponse(
            intent = ExchangeIntent(
                intentId = "fb9488fc-e44c-435c-b074-144b5a288c42",
                clientId = "claude-code",
                sessionId = "session-1",
                walletId = "wallet-1",
                provider = "changelly",
                rateType = RateType.FLOAT,
                fromNetwork = "ethereum",
                fromContractAddress = "0",
                fromDecimals = 18,
                fromAmount = "1000000000000000000",
                fromAddress = "0xFromWalletAddress",
                toNetwork = "tron",
                toContractAddress = "0",
                toDecimals = 6,
                toAmount = null,
                payoutAddress = "TToWalletAddress",
                toExtraId = null,
            ),
            transactions = emptyList(),
        )
        assertThat(actual).isEqualTo(expected)
    }

    @Test
    fun `GIVEN sent transaction with the full exchange status format WHEN parsed THEN extra fields are ignored`() {
        // Arrange
        val json = """
            {
              "intent": {
                "intentId": "intent-1",
                "walletId": "wallet-1",
                "provider": "changelly",
                "rateType": "fixed",
                "fromContractAddress": "0",
                "fromNetwork": "ethereum",
                "fromDecimals": 18,
                "toContractAddress": "0",
                "toNetwork": "tron",
                "toDecimals": 6,
                "toAmount": "2500000",
                "toExtraId": "memo-1"
              },
              "transactions": [
                {
                  "type": "exchange",
                  "txId": "tx-1",
                  "providerId": "changelly",
                  "rateType": "fixed",
                  "status": "finished",
                  "fromNetwork": "ethereum",
                  "fromAmount": "1000000000000000000",
                  "payinAddress": "0xPayin",
                  "createdAt": "2026-10-09T07:00:00.000Z"
                }
              ]
            }
        """.trimIndent()

        // Act
        val actual = adapter.fromJson(json)

        // Assert
        val expected = ExchangeIntentResponse(
            intent = ExchangeIntent(
                intentId = "intent-1",
                clientId = null,
                sessionId = null,
                walletId = "wallet-1",
                provider = "changelly",
                rateType = RateType.FIXED,
                fromNetwork = "ethereum",
                fromContractAddress = "0",
                fromDecimals = 18,
                fromAmount = null,
                fromAddress = null,
                toNetwork = "tron",
                toContractAddress = "0",
                toDecimals = 6,
                toAmount = "2500000",
                payoutAddress = null,
                toExtraId = "memo-1",
            ),
            transactions = listOf(ExchangeIntentTransaction(txId = "tx-1")),
        )
        assertThat(actual).isEqualTo(expected)
    }

    @Test
    fun `GIVEN rate type other than float or fixed WHEN parsed THEN parsing fails`() {
        // Arrange
        val json = """
            {
              "intent": {
                "intentId": "intent-1",
                "walletId": "wallet-1",
                "provider": "changelly",
                "rateType": "sliding",
                "fromContractAddress": "0",
                "fromNetwork": "ethereum",
                "fromDecimals": 18,
                "fromAmount": "1",
                "toContractAddress": "0",
                "toNetwork": "tron",
                "toDecimals": 6
              },
              "transactions": []
            }
        """.trimIndent()

        // Act
        val actual = runCatching { adapter.fromJson(json) }.exceptionOrNull()

        // Assert
        assertThat(actual).isInstanceOf(JsonDataException::class.java)
    }
}