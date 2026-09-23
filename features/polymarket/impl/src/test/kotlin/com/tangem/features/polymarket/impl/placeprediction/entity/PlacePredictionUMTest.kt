package com.tangem.features.polymarket.impl.placeprediction.entity

import com.google.common.truth.Truth.assertThat
import com.tangem.domain.polymarket.model.PredictionOrderSide
import com.tangem.test.core.ProvideTestModels
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.TestInstance
import org.junit.jupiter.params.ParameterizedTest
import java.math.BigDecimal

@TestInstance(TestInstance.Lifecycle.PER_CLASS)
internal class PlacePredictionUMTest {

    @ParameterizedTest
    @ProvideTestModels
    fun sellPosition(model: SellPositionModel) {
        // Arrange
        val initial = PlacePredictionUM.initial(side = PredictionOrderSide.SELL)
        val state = initial.copy(order = (initial.order as OrderSideUM.Sell).copy(position = model.position))

        // Act
        val canQuote = state.canQuote()
        val canPollPrice = state.canPollPrice()
        val available = model.position.availableOrZero

        // Assert
        assertThat(canQuote).isEqualTo(model.expectedCanQuote)
        assertThat(canPollPrice).isEqualTo(model.expectedCanPollPrice)
        assertThat(available).isEqualTo(model.expectedAvailable)
    }

    internal data class SellPositionModel(
        val position: SellPositionUM,
        val expectedCanQuote: Boolean,
        val expectedCanPollPrice: Boolean,
        val expectedAvailable: BigDecimal = BigDecimal.ZERO,
    )

    private fun provideTestModels() = listOf(
        SellPositionModel(
            position = SellPositionUM.Loading,
            expectedCanQuote = false,
            expectedCanPollPrice = true,
        ),
        SellPositionModel(
            position = SellPositionUM.Held(available = BigDecimal("51.23")),
            expectedCanQuote = true,
            expectedCanPollPrice = true,
            expectedAvailable = BigDecimal("51.23"),
        ),
        SellPositionModel(
            position = SellPositionUM.Held(available = BigDecimal.ZERO),
            expectedCanQuote = true,
            expectedCanPollPrice = true,
        ),
        SellPositionModel(
            position = SellPositionUM.NotHeld,
            expectedCanQuote = false,
            expectedCanPollPrice = false,
        ),
        SellPositionModel(
            position = SellPositionUM.Unreadable,
            expectedCanQuote = false,
            expectedCanPollPrice = false,
        ),
    )

    @Test
    fun `GIVEN a buy WHEN the loops are gated THEN neither waits on a position`() {
        // Arrange
        val state = PlacePredictionUM.initial(side = PredictionOrderSide.BUY)

        // Act
        val canQuote = state.canQuote()
        val canPollPrice = state.canPollPrice()

        // Assert
        assertThat(canQuote).isTrue()
        assertThat(canPollPrice).isTrue()
    }
}