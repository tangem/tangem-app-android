package com.tangem.grow.datasource.yield

import com.google.common.truth.Truth.assertThat
import com.squareup.moshi.adapter
import com.tangem.grow.datasource.yield.models.YieldSupplyMarketTokenDto
import com.tangem.test.core.ProvideTestModels
import com.tangem.test.core.datastore.legacyDataStoreMoshi
import com.tangem.test.core.datastore.readStoredJson
import com.tangem.test.core.datastore.writeStoredJson
import org.junit.jupiter.api.TestInstance
import org.junit.jupiter.params.ParameterizedTest
import java.math.BigDecimal

/**
 * Guards the stored format of the yield markets cache: files written by the Moshi serializer must read back unchanged
 * after the store moved to kotlinx, and new writes keep that format, so the Moshi serializer that is still used while
 * the kotlinx DataStore toggle is off reads them too.
 */
@OptIn(ExperimentalStdlibApi::class)
@TestInstance(TestInstance.Lifecycle.PER_CLASS)
internal class YieldMarketsStoredFormatTest {

    @ParameterizedTest
    @ProvideTestModels
    fun `GIVEN markets WHEN encoded by Moshi THEN matches the stored format`(model: FormatModel) {
        // Act
        val actual = legacyDataStoreMoshi.adapter<List<YieldSupplyMarketTokenDto>>().toJson(model.value)

        // Assert
        assertThat(actual).isEqualTo(model.json)
    }

    @ParameterizedTest
    @ProvideTestModels
    fun `GIVEN stored format WHEN decoded by Moshi THEN markets are kept`(model: FormatModel) {
        // Act
        val actual = legacyDataStoreMoshi.adapter<List<YieldSupplyMarketTokenDto>>().fromJson(model.json)

        // Assert
        assertThat(actual).isEqualTo(model.value)
    }

    @ParameterizedTest
    @ProvideTestModels
    fun `GIVEN stored format WHEN read from DataStore THEN markets are kept`(model: FormatModel) {
        // Act
        val actual = readStoredJson(json = model.json, defaultValue = emptyList<YieldSupplyMarketTokenDto>())

        // Assert
        assertThat(actual).isEqualTo(model.value)
    }

    @ParameterizedTest
    @ProvideTestModels
    fun `GIVEN markets WHEN written to DataStore THEN keeps the stored format`(model: FormatModel) {
        // Act
        val actual = writeStoredJson(value = model.value, defaultValue = emptyList())

        // Assert
        assertThat(actual).isEqualTo(model.json)
    }

    private fun provideTestModels() = listOf(
        FormatModel(
            value = listOf(
                YieldSupplyMarketTokenDto(
                    tokenAddress = "0xtoken",
                    tokenSymbol = "USDT",
                    tokenName = "Tether",
                    apy = BigDecimal("4.25"),
                    decimals = 6,
                    isActive = true,
                    chainId = 1,
                    maxFeeNative = BigDecimal("0.00000001"),
                    maxFeeUSD = BigDecimal("0.5"),
                    hasActiveCampaign = false,
                    rewardsApr = BigDecimal("1"),
                    totalApyEst = BigDecimal("5.25"),
                ),
            ),
            json = """[{"tokenAddress":"0xtoken","tokenSymbol":"USDT","tokenName":"Tether","apy":"4.25",""" +
                """"decimals":6,"isActive":true,"chainId":1,"maxFeeNative":"1E-8","maxFeeUSD":"0.5",""" +
                """"hasActiveCampaign":false,"rewardsApr":"1","totalApyEst":"5.25"}]""",
        ),
        FormatModel(
            value = listOf(YieldSupplyMarketTokenDto(tokenAddress = "0xtoken")),
            json = """[{"tokenAddress":"0xtoken"}]""",
        ),
    )

    data class FormatModel(val value: List<YieldSupplyMarketTokenDto>, val json: String)
}