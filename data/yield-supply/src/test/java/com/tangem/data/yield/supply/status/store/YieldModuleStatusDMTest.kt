package com.tangem.data.yield.supply.status.store

import com.google.common.truth.Truth.assertThat
import com.tangem.data.yield.supply.status.createNetwork
import com.tangem.data.yield.supply.status.createStatus
import com.tangem.data.yield.supply.status.createToken
import com.tangem.domain.models.StatusSource
import com.tangem.domain.models.network.Network
import com.tangem.test.core.ProvideTestModels
import kotlinx.serialization.json.Json
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.TestInstance
import org.junit.jupiter.params.ParameterizedTest
import java.math.BigDecimal

@TestInstance(TestInstance.Lifecycle.PER_CLASS)
internal class YieldModuleStatusDMTest {

    @ParameterizedTest
    @ProvideTestModels
    fun `GIVEN derivation path WHEN status stored and restored THEN network id is kept`(path: Network.DerivationPath) {
        // Arrange
        val status = createStatus(
            token = createToken(network = createNetwork(derivationPath = path)),
            isActive = true,
            isInitialized = true,
            isAllowedToSpend = true,
            effectiveProtocolBalance = BigDecimal("1.000001"),
            effectiveBalance = BigDecimal("3.000001"),
        )

        // Act
        val restored = status.toDataModel().toDomain(StatusSource.CACHE)

        // Assert
        assertThat(restored).isEqualTo(status.copy(source = StatusSource.CACHE))
    }

    @Test
    fun `GIVEN stored file WHEN decoded THEN fields are read by their persisted names`() {
        // Arrange — the second entry is the shape written before positions carried the paired total
        val json = """
            {
                "011": [
                    {
                        "network_id": "polygon",
                        "derivation_path_type": "card",
                        "derivation_path_value": "m/44'/60'/0'/0/0",
                        "token_address": "0xabc",
                        "is_active": true,
                        "is_initialized": true,
                        "is_allowed_to_spend": false,
                        "effective_protocol_balance": "5.25",
                        "effective_balance": "8.5"
                    },
                    {
                        "network_id": "base",
                        "derivation_path_type": "none",
                        "token_address": "0xdef",
                        "is_active": false,
                        "is_initialized": false,
                        "is_allowed_to_spend": false
                    }
                ]
            }
        """.trimIndent()

        // Act
        val actual = Json.decodeFromString<WalletIdWithStatusDMs>(json)

        // Assert
        val expected = mapOf(
            "011" to setOf(
                YieldModuleStatusDM(
                    networkId = "polygon",
                    derivationPathType = YieldModuleStatusDM.DerivationPathType.CARD,
                    derivationPathValue = "m/44'/60'/0'/0/0",
                    tokenAddress = "0xabc",
                    isActive = true,
                    isInitialized = true,
                    isAllowedToSpend = false,
                    effectiveProtocolBalance = BigDecimal("5.25"),
                    effectiveBalance = BigDecimal("8.5"),
                ),
                YieldModuleStatusDM(
                    networkId = "base",
                    derivationPathType = YieldModuleStatusDM.DerivationPathType.NONE,
                    tokenAddress = "0xdef",
                    isActive = false,
                    isInitialized = false,
                    isAllowedToSpend = false,
                ),
            ),
        )
        assertThat(actual).isEqualTo(expected)
    }

    private fun provideTestModels() = listOf(
        Network.DerivationPath.Card("m/44'/60'/0'/0/0"),
        Network.DerivationPath.Custom("m/44'/60'/1'/0/0"),
        Network.DerivationPath.Joint("m/44'/60'/888888'/0/0"),
        Network.DerivationPath.None,
    )
}