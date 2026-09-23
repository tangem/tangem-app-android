package com.tangem.domain.polymarket.model

import com.google.common.truth.Truth.assertThat
import com.tangem.test.core.ProvideTestModels
import org.junit.jupiter.api.TestInstance
import org.junit.jupiter.params.ParameterizedTest

@TestInstance(TestInstance.Lifecycle.PER_CLASS)
internal class PredictionOrderSideTest {

    @ParameterizedTest
    @ProvideTestModels
    fun opposite(model: OppositeModel) {
        // Act
        val actual = model.side.opposite()

        // Assert
        assertThat(actual).isEqualTo(model.expected)
    }

    internal data class OppositeModel(val side: PredictionOrderSide, val expected: PredictionOrderSide)

    private fun provideTestModels() = listOf(
        OppositeModel(side = PredictionOrderSide.BUY, expected = PredictionOrderSide.SELL),
        OppositeModel(side = PredictionOrderSide.SELL, expected = PredictionOrderSide.BUY),
    )
}