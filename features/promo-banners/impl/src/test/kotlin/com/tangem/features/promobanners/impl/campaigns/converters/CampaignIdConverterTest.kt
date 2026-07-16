package com.tangem.features.promobanners.impl.campaigns.converters

import com.google.common.truth.Truth.assertThat
import com.tangem.features.promobanners.impl.campaigns.entity.CampaignType
import org.junit.jupiter.api.TestInstance
import org.junit.jupiter.params.ParameterizedTest
import org.junit.jupiter.params.provider.MethodSource

@TestInstance(TestInstance.Lifecycle.PER_CLASS)
internal class CampaignIdConverterTest {

    private val converter = CampaignIdConverter()

    @ParameterizedTest
    @MethodSource("provideConvertModels")
    fun `GIVEN campaign id WHEN convert THEN correct campaign type is returned`(model: ConvertModel) {
        // Act
        val actual = converter.convert(model.id)

        // Assert
        assertThat(actual).isEqualTo(model.expected)
    }

    private fun provideConvertModels() = listOf(
        ConvertModel(
            id = "whale-swap-cashback",
            expected = CampaignType.WhaleSwapCashback(campaignId = "whale-swap-cashback"),
        ),
        ConvertModel(
            id = "reactivation-cashback",
            expected = CampaignType.ReactivationCashback(campaignId = "reactivation-cashback"),
        ),
        ConvertModel(id = "0", expected = null),
        ConvertModel(id = "unknown", expected = null),
        ConvertModel(id = "", expected = null),
    )

    internal data class ConvertModel(
        val id: String,
        val expected: CampaignType?,
    ) {
        override fun toString(): String = "\"$id\" -> ${expected?.let { it::class.simpleName } ?: "null"}"
    }
}