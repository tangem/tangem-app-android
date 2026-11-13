package com.tangem.data.polymarket.converter

import com.google.common.truth.Truth.assertThat
import com.tangem.domain.polymarket.model.PolymarketSoccerCard
import com.tangem.domain.polymarket.model.PolymarketSoccerCard.Button
import com.tangem.domain.polymarket.model.PolymarketSoccerCard.ButtonSide
import com.tangem.domain.polymarket.model.PolymarketSoccerCard.Content
import com.tangem.domain.polymarket.model.PolymarketSoccerCard.Position
import com.tangem.domain.polymarket.model.PolymarketSoccerEvent
import com.tangem.domain.polymarket.model.PolymarketSoccerTab
import com.tangem.domain.polymarket.model.PolymarketStatus
import com.tangem.domain.polymarket.model.PolymarketTeam
import com.tangem.grow.datasource.polymarket.models.PolymarketSoccerTabDto
import org.junit.jupiter.api.Test
import org.junit.jupiter.params.ParameterizedTest
import org.junit.jupiter.params.provider.ValueSource
import java.math.BigDecimal
import java.time.Instant

internal class PolymarketSoccerEventConverterTest {
    @Test
    fun `GIVEN match metadata WHEN converted THEN kickoff and provider team order are preserved`() {
        // Arrange
        val dto = SoccerFixtures.event(emptyList())
        val expected = PolymarketSoccerEvent(
            id = "main", slug = "main-slug", title = "Match", description = "Event rules",
            rulesUrl = "https://polymarket.com", imageUrl = null, iconUrl = null, status = PolymarketStatus.ACTIVE,
            isLive = false, startTime = Instant.parse("2026-11-12T20:00:00Z"), startDate = "2026-10-01T00:00:00Z",
            endDate = null, volume = BigDecimal("123.5"), volume24h = null, liquidity = null, isNegRisk = true,
            gameId = "game", score = "0-0", period = "NS", elapsed = null, isEnded = false,
            dataFetchedAt = Instant.parse("2026-11-12T19:00:00Z"),
            league = PolymarketSoccerEvent.League("epl", "Premier League", null), resolutionSource = "source",
            sportOrdering = "away", teams = listOf(
                PolymarketTeam("Away", "AWY", null, null, "away"),
                PolymarketTeam("Home", "HOM", null, null, "home"),
            ), tabs = emptyList(),
        )

        // Act
        val result = PolymarketSoccerEventConverter.convert(dto)

        // Assert
        assertThat(result).isEqualTo(expected)
    }

    @Test
    fun `GIVEN equal spread labels WHEN converted THEN signed positions and market trading metadata stay distinct`() {
        // Arrange
        val dto = SoccerFixtures.event()

        // Act
        val card = PolymarketSoccerEventConverter.convert(dto).tabs.single().cards.single()

        // Assert
        assertThat(card.content).isEqualTo(
            Content.SidePicker(
                listOf(
                    Position("plus", "1.5", BigDecimal("1.5"), false, listOf(
                        Button(ButtonSide.LEFT, "Label left", "plus", "plus-yes"),
                        Button(ButtonSide.RIGHT, "Label right", "plus", "plus-no"),
                    )),
                    Position("minus", "1.5", BigDecimal("-1.5"), true, listOf(
                        Button(ButtonSide.LEFT, "Label left", "minus", "minus-yes"),
                        Button(ButtonSide.RIGHT, "Label right", "minus", "minus-no"),
                    )),
                ),
            ),
        )
        assertThat(card.markets.map { it.line }).containsExactly(BigDecimal("-1.5"), BigDecimal("-1.5")).inOrder()
        assertThat(card.markets.map { it.isNegRisk }).containsExactly(false, true).inOrder()
        assertThat(card.markets.first().outcomes.map { it.probability }).containsExactly(BigDecimal("0.42"), null).inOrder()
    }

    @ParameterizedTest
    @ValueSource(strings = ["unknown-key", "unknown-template", "missing-market", "missing-outcome", "missing-payload",
        "unknown-side", "no-default", "two-defaults", "cross-market"])
    fun `GIVEN unsupported or malformed card WHEN converted THEN valid siblings remain in BFF order`(case: String) {
        // Arrange
        val valid = SoccerFixtures.spreads()
        val positions = requireNotNull(valid.positions)
        val first = positions.first()
        val malformed = when (case) {
            "unknown-key" -> valid.copy(key = "future")
            "unknown-template" -> valid.copy(template = "future")
            "missing-market" -> valid.copy(markets = emptyList())
            "missing-outcome" -> valid.copy(markets = valid.markets.map { it.copy(outcomes = emptyList()) })
            "missing-payload" -> valid.copy(positions = null)
            "unknown-side" -> valid.copy(positions = listOf(first.copy(isDefault = true,
                buttons = first.buttons.map { it.copy(side = "future") })))
            "no-default" -> valid.copy(positions = positions.map { it.copy(isDefault = false) })
            "two-defaults" -> valid.copy(positions = positions.map { it.copy(isDefault = true) })
            else -> valid.copy(positions = listOf(first.copy(isDefault = true, marketId = "minus")))
        }
        val dto = SoccerFixtures.event().copy(tabs = listOf(
            PolymarketSoccerTabDto("future", listOf(valid)),
            PolymarketSoccerTabDto("first_half", listOf(malformed)),
            PolymarketSoccerTabDto("second_half", listOf(malformed, valid)),
            PolymarketSoccerTabDto("game_lines", listOf(valid)),
        ))

        // Act
        val tabs = PolymarketSoccerEventConverter.convert(dto).tabs

        // Assert
        assertThat(tabs.map { it.key }).containsExactly(
            PolymarketSoccerTab.Key.SECOND_HALF, PolymarketSoccerTab.Key.GAME_LINES,
        ).inOrder()
        assertThat(tabs.map { it.cards.single().key }).containsExactly(
            PolymarketSoccerCard.Key.SPREADS, PolymarketSoccerCard.Key.SPREADS,
        ).inOrder()
    }

    @ParameterizedTest
    @ValueSource(strings = ["missing-buttons", "reordered-buttons", "wrong-yes-no", "missing-rows", "wrong-row-side",
        "missing-team", "unknown-team", "duplicate-position", "duplicate-market"])
    fun `GIVEN malformed template payload WHEN converted THEN card is omitted`(case: String) {
        // Arrange
        val base = SoccerFixtures.spreads()
        val malformed = when (case) {
            "missing-buttons" -> base.copy(template = "three_way", buttons = null)
            "reordered-buttons" -> base.copy(template = "three_way", buttons = listOf(
                SoccerFixtures.button("right", "plus"), SoccerFixtures.button("middle", "plus"),
                SoccerFixtures.button("left", "minus"),
            ))
            "wrong-yes-no" -> base.copy(template = "yes_no", buttons = listOf(
                SoccerFixtures.button("left", "plus"), SoccerFixtures.button("right", "plus", "no"),
            ))
            "missing-rows" -> base.copy(template = "list", rows = null)
            "wrong-row-side" -> base.copy(template = "list", rows = listOf(SoccerFixtures.button("no", "plus")))
            "missing-team" -> base.copy(key = "team_totals", template = "over_under_picker", teamSide = null)
            "unknown-team" -> base.copy(key = "team_totals", template = "over_under_picker", teamSide = "future")
            "duplicate-position" -> base.copy(positions = listOf(
                SoccerFixtures.position("plus", 1.5, true), SoccerFixtures.position("plus", -1.5, false),
            ))
            else -> base.copy(markets = base.markets + base.markets.first())
        }

        // Act
        val tabs = PolymarketSoccerEventConverter.convert(SoccerFixtures.event(listOf(malformed))).tabs

        // Assert
        assertThat(tabs).isEmpty()
    }

    @Test
    fun `GIVEN remaining templates WHEN converted THEN button order and both team totals are preserved`() {
        // Arrange
        val market = SoccerFixtures.market()
        val base = SoccerFixtures.spreads().copy(markets = listOf(market))
        val totalsPosition = SoccerFixtures.position("market", 2.5, true).copy(
            buttons = listOf(SoccerFixtures.button("over"), SoccerFixtures.button("under", outcome = "no")),
        )
        val cards = listOf(
            base.copy(key = "moneyline", template = "three_way",
                markets = listOf(SoccerFixtures.market("home"), SoccerFixtures.market("draw"), SoccerFixtures.market("away")),
                buttons = listOf(
                    SoccerFixtures.button("left", "home"), SoccerFixtures.button("middle", "draw"),
                    SoccerFixtures.button("right", "away"),
                )),
            base.copy(key = "both_teams_to_score", template = "yes_no", buttons = listOf(
                SoccerFixtures.button("yes"), SoccerFixtures.button("no", outcome = "no"),
            )),
            base.copy(key = "exact_score", template = "list", rows = listOf(SoccerFixtures.button("yes"))),
            base.copy(key = "team_totals", template = "over_under_picker", teamSide = "left",
                positions = listOf(totalsPosition)),
            base.copy(key = "team_totals", template = "over_under_picker", teamSide = "right",
                positions = listOf(totalsPosition)),
        )

        // Act
        val result = PolymarketSoccerEventConverter.convert(SoccerFixtures.event(cards)).tabs.single().cards

        // Assert
        assertThat(result.map { it.content }).containsExactly(
            Content.ThreeWay(listOf(
                Button(ButtonSide.LEFT, "Label left", "home", "home-yes"),
                Button(ButtonSide.MIDDLE, "Label middle", "draw", "draw-yes"),
                Button(ButtonSide.RIGHT, "Label right", "away", "away-yes"),
            )),
            Content.YesNo(listOf(
                Button(ButtonSide.YES, "Label yes", "market", "market-yes"),
                Button(ButtonSide.NO, "Label no", "market", "market-no"),
            )),
            Content.Rows(listOf(Button(ButtonSide.YES, "Label yes", "market", "market-yes"))),
            Content.OverUnderPicker(listOf(Position("market", "1.5", BigDecimal("2.5"), true, listOf(
                Button(ButtonSide.OVER, "Label over", "market", "market-yes"),
                Button(ButtonSide.UNDER, "Label under", "market", "market-no"),
            ))), PolymarketSoccerCard.TeamSide.LEFT),
            Content.OverUnderPicker(listOf(Position("market", "1.5", BigDecimal("2.5"), true, listOf(
                Button(ButtonSide.OVER, "Label over", "market", "market-yes"),
                Button(ButtonSide.UNDER, "Label under", "market", "market-no"),
            ))), PolymarketSoccerCard.TeamSide.RIGHT),
        ).inOrder()
    }
}