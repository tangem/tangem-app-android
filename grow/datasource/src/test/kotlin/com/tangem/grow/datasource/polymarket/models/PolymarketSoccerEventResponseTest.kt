package com.tangem.grow.datasource.polymarket.models

import com.google.common.truth.Truth.assertThat
import com.squareup.moshi.Moshi
import org.junit.jupiter.api.Test

internal class PolymarketSoccerEventResponseTest {

    private val moshi = Moshi.Builder().build()
    private val adapter = moshi.adapter(PolymarketSoccerEventResponse::class.java)

    @Test
    fun `GIVEN Notion contract example WHEN decoded THEN all templates retain resolvable outcome references`() {
        // Arrange
        val json = contractExample()

        // Act
        val event = requireNotNull(adapter.fromJson(json)).event
        val cards = event.tabs.flatMap { it.cards }

        // Assert
        assertThat(event.tabs.map { it.key }).containsExactly("game_lines", "exact_score").inOrder()
        assertThat(cards.map { it.template }).containsExactly(
            "three_way", "side_picker", "over_under_picker", "yes_no", "list",
        ).inOrder()
        cards.forEach { card ->
            val buttons = card.buttons.orEmpty() + card.rows.orEmpty() +
                card.positions.orEmpty().flatMap { it.buttons }
            assertThat(buttons).isNotEmpty()
            buttons.forEach { button ->
                val market = card.markets.single { it.id == button.marketId }
                assertThat(market.outcomes.map { it.assetId }).contains(button.assetId)
            }
        }
    }

    @Test
    fun `GIVEN opposite spreads with equal labels WHEN decoded THEN signed lines and default stay distinct`() {
        // Arrange
        val json = contractExample()

        // Act
        val event = requireNotNull(adapter.fromJson(json)).event
        val spreads = event.tabs.first().cards.single { it.key == "spreads" }
        val positions = requireNotNull(spreads.positions)

        // Assert
        assertThat(positions.map { it.pickerLabel }).containsExactly("1.5", "1.5").inOrder()
        assertThat(positions.map { it.line }).containsExactly(1.5, -1.5).inOrder()
        assertThat(positions.map { it.marketId }).containsExactly("5856812", "5856811").inOrder()
        assertThat(positions.single { it.isDefault }.marketId).isEqualTo("5856811")
        assertThat(spreads.markets.map { it.sportsMarketType }).containsExactly("spreads", "spreads")
        assertThat(spreads.markets.map { it.line }).containsExactly(-1.5, -1.5)
    }

    @Test
    fun `GIVEN completed match from dev BFF WHEN decoded THEN all tabs and team totals remain separate`() {
        // Arrange
        // Dev GET sports/soccer/events/1209076, 2026-11-11. Descriptions and repeated positions/rows trimmed.
        val json = requireNotNull(javaClass.getResource("/polymarket/soccer-dev-completed.json")).readText()

        // Act
        val event = requireNotNull(adapter.fromJson(json)).event

        // Assert
        assertThat(event.eventId).isEqualTo("1209076")
        assertThat(event.isEnded).isTrue()
        assertThat(event.score).isEqualTo("2-0")
        assertThat(event.tabs.map { it.key }).containsExactly(
            "game_lines", "exact_score", "first_half", "second_half",
        ).inOrder()
        assertThat(event.tabs.map { it.cards.size }).containsExactly(7, 1, 8, 7).inOrder()
        val teamTotals = event.tabs.first().cards.filter { it.key == "team_totals" }
        assertThat(teamTotals.map { it.teamSide }).containsExactly("left", "right").inOrder()
        val markets = event.tabs.flatMap { it.cards }.flatMap { it.markets }
        assertThat(markets.map { it.isNegRisk }.toSet()).containsExactly(true, false)
        assertThat(markets.all { it.conditionId != null }).isTrue()
    }

    @Test
    fun `GIVEN future template WHEN decoded THEN unknown card does not prevent reading adjacent cards`() {
        // Arrange
        val tabAdapter = moshi.adapter(PolymarketSoccerTabDto::class.java)
        val json = """
            {"key":"future_tab","cards":[
              {"key":"future_card","template":"future_template","future_payload":{"value":1}},
              {"key":"both_teams_to_score","template":"yes_no","sportsMarketType":"both_teams_to_score",
               "buttons":[],"markets":[]}
            ]}
        """.trimIndent()

        // Act
        val tab = requireNotNull(tabAdapter.fromJson(json))

        // Assert
        assertThat(tab.key).isEqualTo("future_tab")
        assertThat(tab.cards.map { it.template }).containsExactly("future_template", "yes_no").inOrder()
        assertThat(tab.cards.first().buttons).isNull()
        assertThat(tab.cards.last().buttons).isEmpty()
    }

    // Documentation example, not a captured live response; schema revision 2026-11-05.
    // https://app.notion.com/p/4095d34eb6788145b4a0dc1efdfb561a
    private fun contractExample(): String = requireNotNull(
        javaClass.getResource("/polymarket/soccer-contract-v1.json"),
    ).readText()
}