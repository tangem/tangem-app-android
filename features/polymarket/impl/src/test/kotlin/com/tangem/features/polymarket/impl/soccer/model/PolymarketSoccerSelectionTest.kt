package com.tangem.features.polymarket.impl.soccer.model

import com.google.common.truth.Truth.assertThat
import com.tangem.domain.polymarket.model.PolymarketSoccerCard
import com.tangem.domain.polymarket.model.PolymarketSoccerTab
import com.tangem.features.polymarket.impl.soccer.ui.state.PolymarketSoccerCardUM
import org.junit.jupiter.api.Test
import org.junit.jupiter.params.ParameterizedTest
import org.junit.jupiter.params.provider.ValueSource

internal class PolymarketSoccerSelectionTest {
    @Test
    fun `GIVEN a fresh match WHEN reconciled THEN BFF default is not stored as a user choice`() {
        // Arrange
        val event = soccerEvent(listOf(soccerPickerCard()))

        // Act
        val result = PolymarketSoccerSelection().reconcile(event)

        // Assert
        assertThat(result).isEqualTo(PolymarketSoccerSelection(
            tab = PolymarketSoccerTab.Key.GAME_LINES,
            positions = emptyMap(),
        ))
    }

    @Test
    fun `GIVEN user selection WHEN tabs and BFF default change THEN each tab keeps its market identity`() {
        // Arrange
        val card = soccerPickerCard()
        val event = soccerEvent(listOf(card)).let {
            it.copy(tabs = it.tabs + PolymarketSoccerTab(PolymarketSoccerTab.Key.FIRST_HALF, listOf(card)))
        }
        val selection = PolymarketSoccerSelection().reconcile(event)
            .selectPosition(event, "game_lines/spreads", "p1")
            .selectTab(event, PolymarketSoccerTab.Key.FIRST_HALF)
            .selectPosition(event, "first_half/spreads", "p2")
        val changed = soccerPickerCard(defaultMarketId = "p1").let {
            it.copy(content = PolymarketSoccerCard.Content.SidePicker(it.pickerPositions()!!.reversed()))
        }
        val refreshed = event.copy(tabs = event.tabs.map { it.copy(cards = listOf(changed)) })

        // Act
        val result = selection.reconcile(refreshed).selectTab(refreshed, PolymarketSoccerTab.Key.GAME_LINES)

        // Assert
        assertThat(result).isEqualTo(PolymarketSoccerSelection(
            tab = PolymarketSoccerTab.Key.GAME_LINES,
            positions = mapOf("game_lines/spreads" to "p1", "first_half/spreads" to "p2"),
        ))
    }

    @Test
    fun `GIVEN a removed position and tab WHEN reconciled THEN new BFF default replaces only missing selections`() {
        // Arrange
        val card = soccerPickerCard()
        val remaining = card.copy(content = PolymarketSoccerCard.Content.SidePicker(
            card.pickerPositions()!!.filter { it.marketId == "p2" },
        ))
        val old = PolymarketSoccerSelection(
            PolymarketSoccerTab.Key.FIRST_HALF,
            mapOf("game_lines/spreads" to "p1", "first_half/spreads" to "p1"),
        )

        // Act
        val result = old.reconcile(soccerEvent(listOf(remaining)))

        // Assert
        assertThat(result).isEqualTo(PolymarketSoccerSelection(
            PolymarketSoccerTab.Key.GAME_LINES, emptyMap(),
        ))
    }

    @Test
    fun `GIVEN two team-total cards WHEN one is changed THEN the other team's choice is preserved`() {
        // Arrange
        val left = soccerPickerCard(PolymarketSoccerCard.Key.TEAM_TOTALS, PolymarketSoccerCard.TeamSide.LEFT)
        val right = soccerPickerCard(PolymarketSoccerCard.Key.TEAM_TOTALS, PolymarketSoccerCard.TeamSide.RIGHT)
        val event = soccerEvent(listOf(left, right))

        // Act
        val result = PolymarketSoccerSelection().reconcile(event)
            .selectPosition(event, "game_lines/team_totals/LEFT", "p2")
            .selectPosition(event, "game_lines/team_totals/RIGHT", "p1")

        // Assert
        assertThat(result).isEqualTo(PolymarketSoccerSelection(
            PolymarketSoccerTab.Key.GAME_LINES,
            mapOf("game_lines/team_totals/LEFT" to "p2", "game_lines/team_totals/RIGHT" to "p1"),
        ))
    }

    @Test
    fun `GIVEN an untouched picker WHEN BFF default changes THEN the displayed selection follows the new snapshot`() {
        // Arrange
        val initial = soccerEvent(listOf(soccerPickerCard()))
        val refreshedCard = soccerPickerCard(defaultMarketId = "p1")
        val refreshed = soccerEvent(listOf(refreshedCard))
        val selected = PolymarketSoccerSelection().reconcile(initial)

        // Act
        val reconciled = selected.reconcile(refreshed)
        val ui = requireNotNull(PolymarketSoccerCardConverter.convert(
            refreshed, PolymarketSoccerTab.Key.GAME_LINES, refreshedCard,
            reconciled.positions["game_lines/spreads"],
        ))

        // Assert
        assertThat(reconciled).isEqualTo(PolymarketSoccerSelection(PolymarketSoccerTab.Key.GAME_LINES))
        assertThat((ui.content as PolymarketSoccerCardUM.Content.Picker)
            .selectedId).isEqualTo("p1")
    }

    @ParameterizedTest
    @ValueSource(strings = ["missing-card", "game_lines/spreads"])
    fun `GIVEN a stale picker callback WHEN selecting THEN state is unchanged`(cardId: String) {
        // Arrange
        val event = soccerEvent(listOf(soccerPickerCard()))
        val selected = PolymarketSoccerSelection().reconcile(event)

        // Act
        val result = selected.selectPosition(event, cardId, "removed-market")

        // Assert
        assertThat(result).isEqualTo(selected)
    }

    @Test
    fun `GIVEN a missing tab WHEN selecting THEN current tab is retained`() {
        // Arrange
        val event = soccerEvent(listOf(soccerPickerCard()))
        val selected = PolymarketSoccerSelection().reconcile(event)

        // Act
        val result = selected.selectTab(event, PolymarketSoccerTab.Key.SECOND_HALF)

        // Assert
        assertThat(result).isEqualTo(selected)
    }

    @Test
    fun `GIVEN no remaining tabs WHEN reconciled THEN stale choices are cleared`() {
        // Arrange
        val event = soccerEvent(listOf(soccerPickerCard()))
        val selected = PolymarketSoccerSelection().reconcile(event)

        // Act
        val result = selected.reconcile(event.copy(tabs = emptyList()))

        // Assert
        assertThat(result).isEqualTo(PolymarketSoccerSelection())
    }
}