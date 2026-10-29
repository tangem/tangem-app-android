package com.tangem.features.polymarket.impl.placeprediction.entity

import com.google.common.truth.Truth.assertThat
import com.tangem.test.core.ProvideTestModels
import com.tangem.utils.StringsSigns.DASH_SIGN
import org.junit.jupiter.api.TestInstance
import org.junit.jupiter.params.ParameterizedTest

@TestInstance(TestInstance.Lifecycle.PER_CLASS)
internal class MarketHeaderUMTest {

    @ParameterizedTest
    @ProvideTestModels
    fun priceLabel(model: PriceLabelModel) {
        // Arrange
        val header = MarketHeaderUM(
            title = "Will it rain?",
            imageUrl = null,
            outcomeTitle = "Yes",
            outcomePrice = model.outcomePrice,
            resolutionDateMillis = null,
            isPriceUnavailable = model.isPriceUnavailable,
            entryPrice = model.entryPrice,
        )

        // Act
        val label = header.priceLabel()

        // Assert
        assertThat(label).isEqualTo(model.expected)
    }

    internal data class PriceLabelModel(
        val outcomePrice: String?,
        val isPriceUnavailable: Boolean,
        val entryPrice: String?,
        val expected: String?,
    )

    private fun provideTestModels() = listOf(
        PriceLabelModel(outcomePrice = null, isPriceUnavailable = true, entryPrice = null, expected = DASH_SIGN),
        PriceLabelModel(outcomePrice = "86¢", isPriceUnavailable = true, entryPrice = null, expected = DASH_SIGN),
        PriceLabelModel(outcomePrice = "86¢", isPriceUnavailable = false, entryPrice = "85¢", expected = "85¢ → 86¢"),
        PriceLabelModel(outcomePrice = "86¢", isPriceUnavailable = false, entryPrice = null, expected = "86¢"),
        PriceLabelModel(
            outcomePrice = null,
            isPriceUnavailable = true,
            entryPrice = "85¢",
            expected = "85¢ → $DASH_SIGN",
        ),
        PriceLabelModel(outcomePrice = null, isPriceUnavailable = false, entryPrice = "85¢", expected = null),
        PriceLabelModel(outcomePrice = null, isPriceUnavailable = false, entryPrice = null, expected = null),
    )
}