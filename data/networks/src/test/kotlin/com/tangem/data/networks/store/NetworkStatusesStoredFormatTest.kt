package com.tangem.data.networks.store

import com.google.common.truth.Truth.assertThat
import com.squareup.moshi.Moshi
import com.squareup.moshi.adapter
import com.tangem.data.networks.entity.NetworkStatusDM
import com.tangem.test.core.ProvideTestModels
import com.tangem.test.core.datastore.legacyDataStoreMoshi
import com.tangem.test.core.datastore.readStoredJson
import com.tangem.test.core.datastore.writeStoredJson
import dev.onenowy.moshipolymorphicadapter.NamePolymorphicAdapterFactory
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.TestInstance
import org.junit.jupiter.params.ParameterizedTest
import java.math.BigDecimal

/**
 * Guards the stored format of the network statuses: files written by the Moshi serializer must read back unchanged
 * after the store moved to kotlinx, and new writes keep that format, so the Moshi serializer that is still used while
 * the kotlinx DataStore toggle is off reads them too.
 */
@OptIn(ExperimentalStdlibApi::class)
@TestInstance(TestInstance.Lifecycle.PER_CLASS)
internal class NetworkStatusesStoredFormatTest {

    /** The production Moshi picks the subtype by key, as registered in `NetworkStatusMoshiModule`. */
    private val moshi: Moshi = legacyDataStoreMoshi.newBuilder()
        .add(
            NamePolymorphicAdapterFactory.of(NetworkStatusDM::class.java)
                .withSubtype(NetworkStatusDM.Verified::class.java, "amounts")
                .withSubtype(NetworkStatusDM.NoAccount::class.java, "amount_to_create_account"),
        )
        .build()

    @ParameterizedTest
    @ProvideTestModels
    fun `GIVEN statuses WHEN encoded by Moshi THEN matches the stored format`(model: FormatModel) {
        // Act
        val actual = moshi.adapter<Map<String, Set<NetworkStatusDM>>>().toJson(model.value)

        // Assert
        assertThat(actual).isEqualTo(model.json)
    }

    @ParameterizedTest
    @ProvideTestModels
    fun `GIVEN stored format WHEN decoded by Moshi THEN statuses are kept`(model: FormatModel) {
        // Act
        val actual = moshi.adapter<Map<String, Set<NetworkStatusDM>>>().fromJson(model.json)

        // Assert
        assertThat(actual).isEqualTo(model.value)
    }

    @ParameterizedTest
    @ProvideTestModels
    fun `GIVEN stored format WHEN read from DataStore THEN statuses are kept`(model: FormatModel) {
        // Act
        val actual = readStoredJson(json = model.json, defaultValue = emptyMap<String, Set<NetworkStatusDM>>())

        // Assert
        assertThat(actual).isEqualTo(model.value)
    }

    @ParameterizedTest
    @ProvideTestModels
    fun `GIVEN statuses WHEN written to DataStore THEN keeps the stored format`(model: FormatModel) {
        // Act
        val actual = writeStoredJson(value = model.value, defaultValue = emptyMap())

        // Assert
        assertThat(actual).isEqualTo(model.json)
    }

    @Test
    fun `GIVEN status without a known key WHEN read from DataStore THEN the file is replaced with the default`() {
        // Arrange
        val json = """{"wallet":[{"network_id":{"value":"ethereum"}}]}"""

        // Act
        val actual = readStoredJson(json = json, defaultValue = emptyMap<String, Set<NetworkStatusDM>>())

        // Assert
        assertThat(actual).isEmpty()
    }

    private fun provideTestModels() = listOf(
        FormatModel(value = mapOf("wallet" to setOf(VERIFIED)), json = """{"wallet":[$VERIFIED_JSON]}"""),
        FormatModel(value = mapOf("wallet" to setOf(NO_ACCOUNT)), json = """{"wallet":[$NO_ACCOUNT_JSON]}"""),
        FormatModel(
            value = mapOf("wallet" to setOf(VERIFIED, NO_ACCOUNT)),
            json = """{"wallet":[$VERIFIED_JSON,$NO_ACCOUNT_JSON]}""",
        ),
    )

    data class FormatModel(val value: Map<String, Set<NetworkStatusDM>>, val json: String)

    private companion object {
        val COIN_ID = NetworkStatusDM.CurrencyId.createCoinId(coinId = "ethereum")
        val TOKEN_ID = NetworkStatusDM.CurrencyId.createTokenId(rawTokenId = "tether", contractAddress = "0xtoken")
        val VERIFIED = NetworkStatusDM.Verified(
            networkId = NetworkStatusDM.ID(value = "ethereum"),
            derivationPath = NetworkStatusDM.DerivationPath(
                value = "m/44'/60'/0'/0/0",
                type = NetworkStatusDM.DerivationPath.Type.CARD,
            ),
            selectedAddress = "0xprimary",
            availableAddresses = setOf(
                NetworkStatusDM.Address(value = "0xprimary", type = NetworkStatusDM.Address.Type.Primary),
                NetworkStatusDM.Address(value = "0xsecondary", type = NetworkStatusDM.Address.Type.Secondary),
            ),
            amounts = listOf(
                NetworkStatusDM.CurrencyAmount(id = COIN_ID, amount = BigDecimal("1.5")),
                NetworkStatusDM.CurrencyAmount(id = TOKEN_ID, amount = BigDecimal("0.00000001")),
            ),
            yieldSupplyStatuses = listOf(
                NetworkStatusDM.YieldSupplyStatus(
                    id = TOKEN_ID,
                    isActive = true,
                    isInitialized = true,
                    isAllowedToSpend = false,
                    effectiveProtocolBalance = BigDecimal("10"),
                ),
                NetworkStatusDM.YieldSupplyStatus(
                    id = COIN_ID,
                    isActive = false,
                    isInitialized = false,
                    isAllowedToSpend = false,
                ),
            ),
        )
        val NO_ACCOUNT = NetworkStatusDM.NoAccount(
            networkId = NetworkStatusDM.ID(value = "xrp"),
            derivationPath = NetworkStatusDM.DerivationPath(
                value = "",
                type = NetworkStatusDM.DerivationPath.Type.NONE,
            ),
            selectedAddress = "raddress",
            availableAddresses = setOf(
                NetworkStatusDM.Address(value = "raddress", type = NetworkStatusDM.Address.Type.Primary),
            ),
            amountToCreateAccount = BigDecimal("1"),
            errorMessage = "Reserve required",
        )
        const val VERIFIED_JSON = """{"network_id":{"value":"ethereum"},""" +
            """"derivation_path":{"value":"m/44'/60'/0'/0/0","type":"card"},"selected_address":"0xprimary",""" +
            """"available_addresses":[{"value":"0xprimary","type":"primary"},""" +
            """{"value":"0xsecondary","type":"secondary"}],"amounts":[{"id":{"value":"ethereum"},"amount":"1.5"},""" +
            """{"id":{"value":"tether⚓0xtoken"},"amount":"1E-8"}],"yield_supply_statuses":[""" +
            """{"id":{"value":"tether⚓0xtoken"},"is_active":true,"is_initialized":true,""" +
            """"is_allowed_to_spend":false,"effective_protocol_balance":"10"},{"id":{"value":"ethereum"},""" +
            """"is_active":false,"is_initialized":false,"is_allowed_to_spend":false}]}"""
        const val NO_ACCOUNT_JSON = """{"network_id":{"value":"xrp"},""" +
            """"derivation_path":{"value":"","type":"none"},""" +
            """"selected_address":"raddress","available_addresses":[{"value":"raddress","type":"primary"}],""" +
            """"amount_to_create_account":"1","error_message":"Reserve required"}"""
    }
}