package com.tangem.features.polymarket.impl.common

import com.google.common.truth.Truth.assertThat
import com.tangem.test.core.ProvideTestModels
import org.junit.jupiter.api.TestInstance
import org.junit.jupiter.params.ParameterizedTest

@TestInstance(TestInstance.Lifecycle.PER_CLASS)
internal class PolymarketOutcomeAccentTest {

    @ParameterizedTest
    @ProvideTestModels
    fun resolveAccent(model: AccentModel) {
        // Act
        val actual = polymarketOutcomeAccent(outcomeIndex = model.outcomeIndex, outcome = model.outcome)

        // Assert
        assertThat(actual).isEqualTo(model.expected)
    }

    private fun provideTestModels() = listOf(
        AccentModel(outcomeIndex = 0, outcome = "Trump", expected = PolymarketOutcomeAccent.Info),
        AccentModel(outcomeIndex = 1, outcome = "Musk", expected = PolymarketOutcomeAccent.Error),
        // The index wins over the label.
        AccentModel(outcomeIndex = 0, outcome = "No", expected = PolymarketOutcomeAccent.Info),
        AccentModel(outcomeIndex = null, outcome = "Yes", expected = PolymarketOutcomeAccent.Info),
        AccentModel(outcomeIndex = null, outcome = "no", expected = PolymarketOutcomeAccent.Error),
        AccentModel(outcomeIndex = null, outcome = "Draw", expected = PolymarketOutcomeAccent.Neutral),
        AccentModel(outcomeIndex = 2, outcome = "Draw", expected = PolymarketOutcomeAccent.Neutral),
    )

    data class AccentModel(val outcomeIndex: Int?, val outcome: String, val expected: PolymarketOutcomeAccent)
}