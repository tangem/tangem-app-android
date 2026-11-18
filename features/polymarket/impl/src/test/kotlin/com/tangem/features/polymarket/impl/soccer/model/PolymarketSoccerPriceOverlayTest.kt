package com.tangem.features.polymarket.impl.soccer.model

import com.google.common.truth.Truth.assertThat
import com.tangem.core.ui.extensions.stringReference
import com.tangem.features.polymarket.impl.common.PolymarketOutcomeAccent
import com.tangem.features.polymarket.impl.soccer.ui.state.PolymarketSoccerCardUM
import kotlinx.collections.immutable.persistentListOf
import org.junit.jupiter.params.ParameterizedTest
import org.junit.jupiter.params.provider.ValueSource
import java.math.BigDecimal

internal class PolymarketSoccerPriceOverlayTest {
    @ParameterizedTest
    @ValueSource(strings = ["buttons", "rows", "picker"])
    fun `GIVEN populated cards WHEN prices overlay THEN only provided prices change including explicit empties`(kind: String) {
        // Arrange
        val outcomes = persistentListOf(
            outcome("left", "42¢"), outcome("middle", "15¢"), outcome("right", "50¢"),
        )
        val updated = persistentListOf(outcome("left", "61¢"), outcome("middle", "—"), outcomes.last())
        val initialContent = when (kind) {
            "buttons" -> PolymarketSoccerCardUM.Content.Buttons(outcomes)
            "rows" -> PolymarketSoccerCardUM.Content.Rows(outcomes)
            else -> PolymarketSoccerCardUM.Content.Picker(persistentListOf(), "selected", outcomes)
        }
        val expectedContent = when (initialContent) {
            is PolymarketSoccerCardUM.Content.Buttons -> initialContent.copy(outcomes = updated)
            is PolymarketSoccerCardUM.Content.Rows -> initialContent.copy(outcomes = updated)
            is PolymarketSoccerCardUM.Content.Picker -> initialContent.copy(outcomes = updated)
        }
        val card = PolymarketSoccerCardUM(
            id = "card", title = stringReference("Title"), iconUrl = "icon", volume = stringReference("$42"),
            rules = PolymarketSoccerCardUM.Rules(stringReference("Title"), "Rules"), content = initialContent,
        )
        val event = soccerEvent(emptyList())
        val screen = PolymarketSoccerConverter {}.convert(event, PolymarketSoccerSelection().reconcile(event))
            .copy(cards = persistentListOf(card), isDescriptionExpanded = true)

        // Act
        val result = screen.withPrices(mapOf("left" to BigDecimal("0.61"), "middle" to null, "other" to BigDecimal.ONE))

        // Assert
        assertThat(result).isEqualTo(screen.copy(cards = persistentListOf(card.copy(content = expectedContent))))
        assertThat(result.tabs).isSameInstanceAs(screen.tabs)
    }

    private fun outcome(assetId: String, price: String) = PolymarketSoccerCardUM.Outcome(
        PolymarketSoccerCardUM.Target("child", "market", assetId), stringReference(assetId),
        stringReference(price), PolymarketOutcomeAccent.Info,
    )
}