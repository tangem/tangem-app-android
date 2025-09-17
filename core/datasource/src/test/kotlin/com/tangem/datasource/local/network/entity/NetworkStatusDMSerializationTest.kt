package com.tangem.datasource.local.network.entity

import com.google.common.truth.Truth
import com.squareup.moshi.Moshi
import com.squareup.moshi.adapter
import com.squareup.moshi.kotlin.reflect.KotlinJsonAdapterFactory
import com.tangem.datasource.api.common.adapter.BigDecimalAdapter
import dev.onenowy.moshipolymorphicadapter.NamePolymorphicAdapterFactory
import org.junit.Test
import java.math.BigDecimal

/**
[REDACTED_AUTHOR]
 */
class NetworkStatusDMSerializationTest {

    private val moshi = Moshi.Builder()
        .add(
            NamePolymorphicAdapterFactory.of(NetworkStatusDM::class.java)
                .withSubtype(NetworkStatusDM.Verified::class.java, "amounts")
                .withSubtype(NetworkStatusDM.NoAccount::class.java, "amount_to_create_account"),
        )
        .add(BigDecimalAdapter())
        .addLast(KotlinJsonAdapterFactory())
        .build()

    @OptIn(ExperimentalStdlibApi::class)
    private val adapter = moshi.adapter<NetworkStatusDM>()

    @Test
    fun `deserialize JSON to Verified`() {
        // Arrange
        val json = """
            {
                "network_id": { "value": "ETH" },
                "derivation_path": {
                    "value": "m/44'/60'/0'/0/0",
                    "type": "card"
                },
                "selected_address": "0x123456",
                "available_addresses": [
                    { "value": "0x123456", "type": "primary" },
                    { "value": "0xabcdef", "type": "secondary" }
                ],
                "amounts": { "ETH": "1.2345" },
                "yield_supply_statuses": { 
                    "ETH": { "is_active": false, "is_initialized": false, "is_allowed_to_spend": false }
                }
            }
        """.trimIndent()

        // Act
        val result = adapter.fromJson(json)

        // Assert
        val expected = NetworkStatusDM.Verified(
            networkId = NetworkStatusDM.ID("ETH"),
            derivationPath = NetworkStatusDM.DerivationPath(
                value = "m/44'/60'/0'/0/0",
                type = NetworkStatusDM.DerivationPath.Type.CARD,
            ),
            selectedAddress = "0x123456",
            availableAddresses = setOf(
                NetworkStatusDM.Address("0x123456", NetworkStatusDM.Address.Type.Primary),
                NetworkStatusDM.Address("0xabcdef", NetworkStatusDM.Address.Type.Secondary),
            ),
            amounts = mapOf("ETH" to BigDecimal("1.2345")),
            yieldSupplyStatuses = mapOf(
                "ETH" to NetworkStatusDM.YieldSupplyStatus(
                    isActive = false,
                    isInitialized = false,
                    isAllowedToSpend = false,
                ),
            ),
        )

        Truth.assertThat(result).isEqualTo(expected)
    }

    @Test
    fun `serialize Verified to JSON`() {
        // Arrange
        val model = NetworkStatusDM.Verified(
            networkId = NetworkStatusDM.ID("ETH"),
            derivationPath = NetworkStatusDM.DerivationPath(
                value = "m/44'/60'/0'/0/0",
                type = NetworkStatusDM.DerivationPath.Type.CARD,
            ),
            selectedAddress = "0x123456",
            availableAddresses = setOf(
                NetworkStatusDM.Address("0x123456", NetworkStatusDM.Address.Type.Primary),
                NetworkStatusDM.Address("0xabcdef", NetworkStatusDM.Address.Type.Secondary),
            ),
            amounts = mapOf("ETH" to BigDecimal("1.2345")),
            yieldSupplyStatuses = mapOf(
                "ETH" to NetworkStatusDM.YieldSupplyStatus(
                    isActive = false,
                    isInitialized = false,
                    isAllowedToSpend = false,
                ),
            ),
        )

        // Act
        val actual = adapter.toJson(model)

        // Assert
        val expected = """
            {
                "network_id": { "value": "ETH" },
                "derivation_path": {
                    "value": "m/44'/60'/0'/0/0",
                    "type": "card"
                },
                "selected_address": "0x123456",
                "available_addresses": [
                    { "value": "0x123456", "type": "primary" },
                    { "value": "0xabcdef", "type": "secondary" }
                ],
                "amounts": { "ETH": "1.2345" },
                "yield_supply_statuses": { 
                    "ETH": { "is_active": false, "is_initialized": false, "is_allowed_to_spend": false }
                }
            }
        """.stripJsonWhitespace()

        Truth.assertThat(actual).isEqualTo(expected)
    }

    @Test
    fun `deserialize JSON to NoAccount`() {
        // Arrange
        val json = """
            {
                "network_id": { "value": "ETH" },
                "derivation_path": {
                    "value": "m/44'/60'/0'/0/0",
                    "type": "card"
                },
                "selected_address": "0x123456",
                "available_addresses": [
                    { "value": "0x123456", "type": "primary" },
                    { "value": "0xabcdef", "type": "secondary" }
                ],
                "amount_to_create_account": "0.05",
                "error_message": "Account not found"
            }
        """.trimIndent()

        // Act
        val result = adapter.fromJson(json)

        // Assert
        val expected = NetworkStatusDM.NoAccount(
            networkId = NetworkStatusDM.ID("ETH"),
            derivationPath = NetworkStatusDM.DerivationPath(
                value = "m/44'/60'/0'/0/0",
                type = NetworkStatusDM.DerivationPath.Type.CARD,
            ),
            selectedAddress = "0x123456",
            availableAddresses = setOf(
                NetworkStatusDM.Address("0x123456", NetworkStatusDM.Address.Type.Primary),
                NetworkStatusDM.Address("0xabcdef", NetworkStatusDM.Address.Type.Secondary),
            ),
            amountToCreateAccount = BigDecimal("0.05"),
            errorMessage = "Account not found",
        )

        Truth.assertThat(result).isEqualTo(expected)
    }

    @Test
    fun `serialize NoAccount to JSON`() {
        // Arrange
        val model = NetworkStatusDM.NoAccount(
            networkId = NetworkStatusDM.ID("ETH"),
            derivationPath = NetworkStatusDM.DerivationPath(
                value = "m/44'/60'/0'/0/0",
                type = NetworkStatusDM.DerivationPath.Type.CARD,
            ),
            selectedAddress = "0x123456",
            availableAddresses = setOf(
                NetworkStatusDM.Address("0x123456", NetworkStatusDM.Address.Type.Primary),
                NetworkStatusDM.Address("0xabcdef", NetworkStatusDM.Address.Type.Secondary),
            ),
            amountToCreateAccount = BigDecimal("0.05"),
            errorMessage = "error",
        )

        // Act
        val actual = adapter.toJson(model)

        // Assert
        val expected = """
            {
                "network_id": { "value": "ETH" },
                "derivation_path": {
                    "value": "m/44'/60'/0'/0/0",
                    "type": "card"
                },
                "selected_address": "0x123456",
                "available_addresses": [
                    { "value": "0x123456", "type": "primary" },
                    { "value": "0xabcdef", "type": "secondary" }
                ],
                "amount_to_create_account": "0.05",
                "error_message": "error"
            }
        """.stripJsonWhitespace()

        Truth.assertThat(actual).isEqualTo(expected)
    }

    private fun String.stripJsonWhitespace(): String = replace(regex = "\\s".toRegex(), replacement = "")
}