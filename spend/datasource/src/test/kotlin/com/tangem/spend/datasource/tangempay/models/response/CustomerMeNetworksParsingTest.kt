package com.tangem.spend.datasource.tangempay.models.response

import com.google.common.truth.Truth.assertThat
import com.tangem.core.remote.json.NetworkJsonFactory
import org.junit.jupiter.api.Test
import java.math.BigDecimal

internal class CustomerMeNetworksParsingTest {

    private val json = NetworkJsonFactory.create()

    @Test
    fun `GIVEN networks json WHEN parse THEN maps enabled and disabled entries`() {
        // Arrange
        val body = """
            {
              "id": "c1", "state": "ACTIVE", "created_at": "2026-01-01T00:00:00Z",
              "product_instances": [], "cards": [],
              "balance": {
                "networks": [
                  {
                    "name": "base", "is_testnet": true, "chain_id": 84532, "status": "ENABLED",
                    "deposit_address": "0xEED",
                    "tokens": [ { "token": "USDC", "token_contract_address": "0x036", "available_for_withdrawal": 6 } ]
                  },
                  {
                    "name": "tron", "is_testnet": false, "chain_id": 728126428, "status": "DISABLED",
                    "deposit_address": null,
                    "tokens": [ { "token": "USDT", "token_contract_address": "TR7", "available_for_withdrawal": null } ]
                  }
                ]
              }
            }
        """.trimIndent()

        // Act
        val result = json.decodeFromString(CustomerMeResponse.Result.serializer(), body)

        // Assert
        assertThat(result.balance!!.networks).containsExactly(
            BalanceResponse.NetworkResponse(
                name = "base",
                isTestnet = true,
                chainId = 84532L,
                status = "ENABLED",
                depositAddress = "0xEED",
                tokens = listOf(
                    BalanceResponse.NetworkTokenResponse(
                        token = "USDC",
                        tokenContractAddress = "0x036",
                        availableForWithdrawal = BigDecimal("6"),
                    ),
                ),
            ),
            BalanceResponse.NetworkResponse(
                name = "tron",
                isTestnet = false,
                chainId = 728126428L,
                status = "DISABLED",
                depositAddress = null,
                tokens = listOf(
                    BalanceResponse.NetworkTokenResponse(
                        token = "USDT",
                        tokenContractAddress = "TR7",
                        availableForWithdrawal = null,
                    ),
                ),
            ),
        ).inOrder()
    }

    @Test
    fun `GIVEN not issued network WHEN parse THEN it has no deposit address and an empty token list`() {
        // Arrange
        // A NOT_ISSUED network has no contract yet, so the backend reports it without a deposit address and
        // with an empty token list — the entry itself must still survive.
        val body = """
            {
              "id": "c1", "state": "ACTIVE", "created_at": "2026-01-01T00:00:00Z",
              "product_instances": [], "cards": [],
              "balance": {
                "networks": [
                  { "name": "base", "is_testnet": true, "chain_id": 84532, "status": "ENABLED",
                    "deposit_address": "0xEED",
                    "tokens": [ { "token": "USDC", "token_contract_address": "0x036" } ] },
                  { "name": "ethereum", "is_testnet": true, "chain_id": 11155111, "status": "NOT_ISSUED",
                    "deposit_address": null, "tokens": [] }
                ]
              }
            }
        """.trimIndent()

        // Act
        val networks = json.decodeFromString(CustomerMeResponse.Result.serializer(), body).balance!!.networks!!

        // Assert
        assertThat(networks).containsExactly(
            BalanceResponse.NetworkResponse(
                name = "base",
                isTestnet = true,
                chainId = 84532L,
                status = "ENABLED",
                depositAddress = "0xEED",
                tokens = listOf(
                    BalanceResponse.NetworkTokenResponse(
                        token = "USDC",
                        tokenContractAddress = "0x036",
                        availableForWithdrawal = null,
                    ),
                ),
            ),
            BalanceResponse.NetworkResponse(
                name = "ethereum",
                isTestnet = true,
                chainId = 11155111L,
                status = "NOT_ISSUED",
                depositAddress = null,
                tokens = emptyList(),
            ),
        ).inOrder()
    }

    @Test
    fun `GIVEN token without contract address WHEN parse THEN contract address is null`() {
        // Arrange
        val body = """
            {
              "id": "c1", "state": "ACTIVE", "created_at": "2026-01-01T00:00:00Z",
              "product_instances": [], "cards": [],
              "balance": {
                "networks": [
                  { "name": "base", "is_testnet": true, "chain_id": 84532, "status": "NOT_ISSUED",
                    "tokens": [ { "token": "USDC" } ] }
                ]
              }
            }
        """.trimIndent()

        // Act
        val result = json.decodeFromString(CustomerMeResponse.Result.serializer(), body)
        val token = result.balance!!.networks!![0].tokens[0]

        // Assert
        val expected = BalanceResponse.NetworkTokenResponse(
            token = "USDC",
            tokenContractAddress = null,
            availableForWithdrawal = null,
        )
        assertThat(token).isEqualTo(expected)
    }

    @Test
    fun `GIVEN balance json without networks WHEN parse THEN networks is null`() {
        // Arrange
        val body = """
            { "id": "c1", "state": "ACTIVE", "created_at": "2026-01-01T00:00:00Z",
              "product_instances": [], "cards": [],
              "balance": {} }
        """.trimIndent()

        // Act
        val result = json.decodeFromString(CustomerMeResponse.Result.serializer(), body)

        // Assert
        assertThat(result.balance!!.networks).isNull()
    }

    @Test
    fun `GIVEN json without balance WHEN parse THEN balance is null`() {
        // Arrange
        val body = """
            { "id": "c1", "state": "ACTIVE", "created_at": "2026-01-01T00:00:00Z",
              "product_instances": [], "cards": [] }
        """.trimIndent()

        // Act
        val result = json.decodeFromString(CustomerMeResponse.Result.serializer(), body)

        // Assert
        assertThat(result.balance).isNull()
    }
}