package com.tangem.data.assetsdiscovery.store

import com.google.common.truth.Truth.assertThat
import com.squareup.moshi.adapter
import com.tangem.datasource.api.tangemTech.models.UserTokensResponse
import com.tangem.test.core.ProvideTestModels
import com.tangem.test.core.datastore.legacyDataStoreMoshi
import com.tangem.test.core.datastore.readStoredJson
import com.tangem.test.core.datastore.writeStoredJson
import org.junit.jupiter.api.TestInstance
import org.junit.jupiter.params.ParameterizedTest

/**
 * Guards the stored format of the discovered tokens: files written by the Moshi serializer must read back unchanged
 * after the store moved to kotlinx, and new writes keep that format, so the Moshi serializer that is still used while
 * the kotlinx DataStore toggle is off reads them too.
 *
 * Values are compared by `toString`, because [UserTokensResponse.Token.equals] ignores most of its fields.
 */
@OptIn(ExperimentalStdlibApi::class)
@TestInstance(TestInstance.Lifecycle.PER_CLASS)
internal class TokenSyncStoredFormatTest {

    @ParameterizedTest
    @ProvideTestModels
    fun `GIVEN tokens WHEN encoded by Moshi THEN matches the stored format`(model: FormatModel) {
        // Act
        val actual = legacyDataStoreMoshi.adapter<List<UserTokensResponse.Token>>().toJson(model.value)

        // Assert
        assertThat(actual).isEqualTo(model.json)
    }

    @ParameterizedTest
    @ProvideTestModels
    fun `GIVEN stored format WHEN decoded by Moshi THEN tokens are kept`(model: FormatModel) {
        // Act
        val actual = legacyDataStoreMoshi.adapter<List<UserTokensResponse.Token>>().fromJson(model.json)

        // Assert
        assertThat(actual.toString()).isEqualTo(model.value.toString())
    }

    @ParameterizedTest
    @ProvideTestModels
    fun `GIVEN stored format WHEN read from DataStore THEN tokens are kept`(model: FormatModel) {
        // Act
        val actual = readStoredJson(json = model.json, defaultValue = emptyList<UserTokensResponse.Token>())

        // Assert
        assertThat(actual.toString()).isEqualTo(model.value.toString())
    }

    @ParameterizedTest
    @ProvideTestModels
    fun `GIVEN tokens WHEN written to DataStore THEN keeps the stored format`(model: FormatModel) {
        // Act
        val actual = writeStoredJson(value = model.value, defaultValue = emptyList())

        // Assert
        assertThat(actual).isEqualTo(model.json)
    }

    private fun provideTestModels() = listOf(
        FormatModel(
            value = listOf(TOKEN),
            json = """[{"id":"tether","accountId":"account","networkId":"ethereum",""" +
                """"derivationPath":"m/44'/60'/0'/0/0","name":"Tether","symbol":"USDT","decimals":6,""" +
                """"contractAddress":"0xtoken","addresses":["0xaddress"],"dynamicAddressesEnabled":true}]""",
        ),
        FormatModel(
            value = listOf(
                TOKEN.copy(
                    id = null,
                    accountId = null,
                    derivationPath = null,
                    contractAddress = null,
                    addresses = null,
                    dynamicAddressesEnabled = null,
                ),
            ),
            json = """[{"networkId":"ethereum","name":"Tether","symbol":"USDT","decimals":6}]""",
        ),
    )

    data class FormatModel(val value: List<UserTokensResponse.Token>, val json: String)

    private companion object {
        val TOKEN = UserTokensResponse.Token(
            id = "tether",
            accountId = "account",
            networkId = "ethereum",
            derivationPath = "m/44'/60'/0'/0/0",
            name = "Tether",
            symbol = "USDT",
            decimals = 6,
            contractAddress = "0xtoken",
            addresses = listOf("0xaddress"),
            dynamicAddressesEnabled = true,
        )
    }
}