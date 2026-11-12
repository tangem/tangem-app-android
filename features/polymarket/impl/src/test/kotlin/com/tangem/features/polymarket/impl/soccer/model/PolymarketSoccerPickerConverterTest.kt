package com.tangem.features.polymarket.impl.soccer.model

import com.google.common.truth.Truth.assertThat
import com.tangem.core.res.R
import com.tangem.core.ui.extensions.combinedReference
import com.tangem.core.ui.extensions.resourceReference
import com.tangem.core.ui.extensions.stringReference
import com.tangem.core.ui.extensions.wrappedList
import com.tangem.domain.polymarket.model.PolymarketSoccerCard
import com.tangem.domain.polymarket.model.PolymarketSoccerTab
import com.tangem.features.polymarket.impl.common.PolymarketOutcomeAccent
import com.tangem.features.polymarket.impl.common.ui.counter.PolymarketCounter
import com.tangem.features.polymarket.impl.soccer.ui.state.PolymarketSoccerCardUM
import kotlinx.collections.immutable.persistentListOf
import org.junit.jupiter.api.Test
import org.junit.jupiter.params.ParameterizedTest
import org.junit.jupiter.params.provider.EnumSource

internal class PolymarketSoccerPickerConverterTest {
    @Test
    fun `GIVEN a selected spread WHEN converted THEN signs targets prices and rules belong to that market`() {
        // Arrange
        val card = soccerPickerCard()
        val title = resourceReference(R.string.prediction_match_card_spreads)
        val expected = PolymarketSoccerCardUM(
            id = "game_lines/spreads", title = title, iconUrl = "match.png", volume = stringReference("—"),
            rules = PolymarketSoccerCardUM.Rules(title, "Rules p1"),
            content = PolymarketSoccerCardUM.Content.Picker(
                items = persistentListOf(PolymarketCounter.Item("p1", "1.5"), PolymarketCounter.Item("p2", "1.5")),
                selectedId = "p1",
                outcomes = persistentListOf(
                    PolymarketSoccerCardUM.Outcome(
                        PolymarketSoccerCardUM.Target("child-p1", "p1", "p1-left"),
                        stringReference("HOM +1.5"), stringReference("—"), PolymarketOutcomeAccent.Info,
                    ),
                    PolymarketSoccerCardUM.Outcome(
                        PolymarketSoccerCardUM.Target("child-p1", "p1", "p1-right"),
                        stringReference("AWY -1.5"), stringReference("70¢"), PolymarketOutcomeAccent.Error,
                    ),
                ),
            ),
        )

        // Act
        val actual = PolymarketSoccerCardConverter.convert(
            soccerEvent(listOf(card)), PolymarketSoccerTab.Key.GAME_LINES, card, "p1",
        )

        // Assert
        assertThat(actual).isEqualTo(expected)
    }

    @ParameterizedTest
    @EnumSource(PolymarketSoccerCard.TeamSide::class)
    fun `GIVEN team totals WHEN converted THEN BFF side names the team and over-under labels are localized`(
        side: PolymarketSoccerCard.TeamSide,
    ) {
        // Arrange
        val card = soccerPickerCard(PolymarketSoccerCard.Key.TEAM_TOTALS, side)
        val event = soccerEvent(listOf(card)).let {
            it.copy(teams = it.teams.map { team -> team.copy(ordering = null) })
        }
        val name = if (side == PolymarketSoccerCard.TeamSide.LEFT) "Home" else "Away"
        val title = resourceReference(R.string.prediction_match_card_team_totals, wrappedList(name))
        val expected = PolymarketSoccerCardUM(
            id = "first_half/team_totals/${side.name}", title = title, iconUrl = "match.png", volume = stringReference("—"),
            rules = PolymarketSoccerCardUM.Rules(title, "Rules p2"),
            content = PolymarketSoccerCardUM.Content.Picker(
                items = persistentListOf(PolymarketCounter.Item("p1", "1.5"), PolymarketCounter.Item("p2", "2.5")),
                selectedId = "p2",
                outcomes = persistentListOf(
                    PolymarketSoccerCardUM.Outcome(
                        PolymarketSoccerCardUM.Target("child-p2", "p2", "p2-left"),
                        combinedReference(resourceReference(R.string.prediction_match_over_short), stringReference(" 2.5")),
                        stringReference("—"), PolymarketOutcomeAccent.Info,
                    ),
                    PolymarketSoccerCardUM.Outcome(
                        PolymarketSoccerCardUM.Target("child-p2", "p2", "p2-right"),
                        combinedReference(resourceReference(R.string.prediction_match_under_short), stringReference(" 2.5")),
                        stringReference("70¢"), PolymarketOutcomeAccent.Error,
                    ),
                ),
            ),
        )

        // Act
        val actual = PolymarketSoccerCardConverter.convert(event, PolymarketSoccerTab.Key.FIRST_HALF, card)

        // Assert
        assertThat(actual).isEqualTo(expected)
    }

    @Test
    fun `GIVEN an obsolete selected market WHEN converting totals THEN current BFF default is shown`() {
        // Arrange
        val card = soccerPickerCard(PolymarketSoccerCard.Key.TOTALS)

        // Act
        val actual = requireNotNull(PolymarketSoccerCardConverter.convert(
            soccerEvent(listOf(card)), PolymarketSoccerTab.Key.GAME_LINES, card, "removed",
        ))

        // Assert
        assertThat(actual.title).isEqualTo(resourceReference(R.string.prediction_match_card_totals))
        assertThat((actual.content as PolymarketSoccerCardUM.Content.Picker).selectedId).isEqualTo("p2")
        assertThat(actual.rules).isEqualTo(PolymarketSoccerCardUM.Rules(actual.title, "Rules p2"))
    }
}