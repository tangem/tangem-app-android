package com.tangem.datasource.local.token

import com.google.common.truth.Truth.assertThat
import com.squareup.moshi.adapter
import com.tangem.grow.datasource.express.models.response.Asset
import com.tangem.test.core.ProvideTestModels
import com.tangem.test.core.datastore.legacyDataStoreMoshi
import com.tangem.test.core.datastore.readStoredJson
import com.tangem.test.core.datastore.writeStoredJson
import org.junit.jupiter.api.TestInstance
import org.junit.jupiter.params.ParameterizedTest

/**
 * Guards the stored format of the express assets: files written by the Moshi serializer must read back unchanged
 * after the store moved to kotlinx, and new writes keep that format.
 */
@OptIn(ExperimentalStdlibApi::class)
@TestInstance(TestInstance.Lifecycle.PER_CLASS)
internal class ExpressAssetsStoredFormatTest {

    @ParameterizedTest
    @ProvideTestModels
    fun `GIVEN assets WHEN encoded by Moshi THEN matches the stored format`(model: FormatModel) {
        // Act
        val actual = legacyDataStoreMoshi.adapter<Map<String, List<Asset>>>().toJson(model.value)

        // Assert
        assertThat(actual).isEqualTo(model.json)
    }

    @ParameterizedTest
    @ProvideTestModels
    fun `GIVEN stored format WHEN read from DataStore THEN assets are kept`(model: FormatModel) {
        // Act
        val actual = readStoredJson(json = model.json, defaultValue = emptyMap<String, List<Asset>>())

        // Assert
        assertThat(actual).isEqualTo(model.value)
    }

    @ParameterizedTest
    @ProvideTestModels
    fun `GIVEN assets WHEN written to DataStore THEN keeps the stored format`(model: FormatModel) {
        // Act
        val actual = writeStoredJson(value = model.value, defaultValue = emptyMap())

        // Assert
        assertThat(actual).isEqualTo(model.json)
    }

    private fun provideTestModels() = listOf(
        FormatModel(
            value = mapOf("wallet" to listOf(ASSET)),
            json = """{"wallet":[{$ASSET_HEAD,"onrampAvailable":true}]}""",
        ),
        FormatModel(
            value = mapOf("wallet" to listOf(ASSET.copy(onrampAvailable = null))),
            json = """{"wallet":[{$ASSET_HEAD}]}""",
        ),
    )

    data class FormatModel(val value: Map<String, List<Asset>>, val json: String)

    private companion object {
        val ASSET = Asset(
            contractAddress = "0xtoken",
            network = "ethereum",
            exchangeAvailable = true,
            onrampAvailable = true,
        )
        const val ASSET_HEAD = """"contractAddress":"0xtoken","network":"ethereum","exchangeAvailable":true"""
    }
}