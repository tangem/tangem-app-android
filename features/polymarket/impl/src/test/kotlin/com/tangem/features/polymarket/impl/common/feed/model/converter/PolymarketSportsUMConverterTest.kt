package com.tangem.features.polymarket.impl.common.feed.model.converter

import com.google.common.truth.Truth.assertThat
import com.tangem.core.res.R
import com.tangem.core.ui.extensions.resourceReference
import com.tangem.core.ui.extensions.stringReference
import com.tangem.domain.polymarket.model.PolymarketSportsState
import com.tangem.domain.polymarket.model.PolymarketTeam
import com.tangem.features.polymarket.impl.createPolymarketEvent
import com.tangem.features.polymarket.impl.createPolymarketMarket
import com.tangem.features.polymarket.impl.createPolymarketOutcome
import com.tangem.features.polymarket.impl.common.feed.ui.state.PolymarketSportsUM
import org.junit.jupiter.api.Test
import org.junit.jupiter.params.ParameterizedTest
import org.junit.jupiter.params.provider.ValueSource
import java.math.BigDecimal

internal class PolymarketSportsUMConverterTest {

    private val home = PolymarketTeam("São Paulo", "SAO1", "Trícolor", "home.png", "home")
    private val away = PolymarketTeam("Away", "AWY", null, "away.png", "away")

    @Test
    fun `GIVEN domain sides and oriented score WHEN mapped THEN presentation uses them directly`() {
        // Arrange
        val game = createGame()

        // Act
        val actual = PolymarketSportsUMConverter(false) { _, _, _ -> }.convert(game)

        // Assert
        assertThat(actual).isEqualTo(PolymarketSportsUM(
            home = PolymarketSportsUM.Team(stringReference("São Paulo"), "home.png"),
            away = PolymarketSportsUM.Team(stringReference("Away"), "away.png"),
            homeScore = "1", awayScore = "2", period = "Q3", isLive = true,
        ))
    }

    @Test
    fun `GIVEN unresolved domain score WHEN mapped THEN numbers are not guessed`() {
        // Arrange
        val game = createGame().copy(score = null)

        // Act
        val actual = PolymarketSportsUMConverter(false) { _, _, _ -> }.convert(game)

        // Assert
        assertThat(actual.homeScore).isNull()
        assertThat(actual.awayScore).isNull()
    }

    @Test
    fun `GIVEN missing REST ordering WHEN domain resolves sides THEN the sports card uses those sides`() {
        // Arrange
        val event = createEvent().copy(teams = listOf(away.copy(ordering = null), home.copy(ordering = null)))
        val game = createGame().copy(
            home = PolymarketSportsState.Team(event.teams!![1], 1),
            away = PolymarketSportsState.Team(event.teams!![0], 0),
        )

        // Act
        val actual = PolymarketEventUMConverter({}, { _, _, _ -> }, true).convert(event, game)

        // Assert
        assertThat(actual.sports?.homeScore).isEqualTo("1")
        assertThat(actual.sports?.awayScore).isEqualTo("2")
        assertThat(actual.rows.single().outcomes.map { it.assetId }).containsExactly("home", "away").inOrder()
    }

    @Test
    fun `GIVEN a reordered REST snapshot WHEN domain still has old indices THEN buttons are not reversed`() {
        // Arrange
        val event = createEvent().copy(teams = listOf(home, away))

        // Act
        val actual = PolymarketEventUMConverter({}, { _, _, _ -> }, true).convert(event, createGame())

        // Assert
        assertThat(actual.sports).isNull()
        assertThat(actual.rows.single().outcomes.map { it.assetId }).containsExactly("away", "home").inOrder()
    }

    @ParameterizedTest
    @ValueSource(strings = ["CAN", "POST", "INT", "AB", "int", "Post"])
    fun `GIVEN stopped provider period WHEN mapped THEN only the Live badge is hidden`(period: String) {
        // Arrange
        val game = createGame().copy(period = period)

        // Act
        val actual = PolymarketEventUMConverter({}, { _, _, _ -> }, true).convert(createEvent(), game)

        // Assert
        assertThat(actual.sports?.isLive).isFalse()
        assertThat(actual.sports?.period).isEqualTo(period)
        assertThat(actual.rows.single().outcomes).hasSize(2)
    }

    @ParameterizedTest
    @ValueSource(strings = ["SAO PAULO", " São Paulo ", "tricolor"])
    fun `GIVEN normalized team labels WHEN mapped THEN sports header and outcome titles agree`(label: String) {
        // Arrange
        val event = createEvent(label)
        val clicks = mutableListOf<Triple<String, String, String>>()
        val converter = PolymarketEventUMConverter(
            onEventClick = {},
            onOutcomeClick = { eventId, marketId, assetId -> clicks += Triple(eventId, marketId, assetId) },
            isPhaseTwoEnabled = true,
        )

        // Act
        val actual = converter.convert(event, createGame())
        val outcomes = actual.rows.single().outcomes
        outcomes.forEach { it.onClick() }

        // Assert
        assertThat(actual.sports?.home?.name).isEqualTo(stringReference("São Paulo"))
        assertThat(outcomes.map { it.title }).containsExactly(stringReference("SAO1"), stringReference("AWY")).inOrder()
        assertThat(outcomes.map { it.price }).containsExactly(stringReference("60¢"), stringReference("40¢")).inOrder()
        assertThat(clicks).containsExactly(
            Triple("event-1", "market-1", "home"), Triple("event-1", "market-1", "away"),
        ).inOrder()
    }

    @Test
    fun `GIVEN one missing team abbreviation WHEN mapping sports buttons THEN both use full team names`() {
        // Arrange
        val other = away.copy(abbreviation = null)
        val event = createEvent("Tricolor").copy(teams = listOf(home, other))
        val game = createGame().copy(
            home = PolymarketSportsState.Team(home, 0),
            away = PolymarketSportsState.Team(other, 1),
        )

        // Act
        val actual = PolymarketEventUMConverter({}, { _, _, _ -> }, isPhaseTwoEnabled = true).convert(event, game)

        // Assert
        assertThat(actual.rows.single().outcomes.map { it.title })
            .containsExactly(stringReference("São Paulo"), stringReference("Away")).inOrder()
    }

    @ParameterizedTest
    @ValueSource(strings = ["SAO1", "Unknown"])
    fun `GIVEN a label matching neither team name nor alias WHEN mapped THEN the ordinary card stays`(label: String) {
        // Arrange
        val event = createEvent(label)

        // Act
        val actual = PolymarketEventUMConverter({}, { _, _, _ -> }, isPhaseTwoEnabled = true).convert(event, createGame())

        // Assert
        assertThat(actual.sports).isNull()
        assertThat(actual.rows.single().outcomes.map { it.title })
            .containsExactly(stringReference("Away"), stringReference(label)).inOrder()
    }

    @ParameterizedTest
    @ValueSource(strings = ["Draw", "Tie", "Draw (São Paulo vs Away)"])
    fun `GIVEN a single market with three outcomes WHEN mapped THEN it is not an iOS moneyline`(draw: String) {
        // Arrange
        val event = createEvent()
        val market = event.markets.single()
        val withDraw = event.copy(markets = listOf(market.copy(outcomes = market.outcomes + createPolymarketOutcome(title = draw))))

        // Act
        val actual = PolymarketEventUMConverter({}, { _, _, _ -> }, isPhaseTwoEnabled = true).convert(withDraw, createGame())

        // Assert
        assertThat(actual.sports).isNull()
        assertThat(actual.rows.single().outcomes).hasSize(3)
    }

    @ParameterizedTest
    @ValueSource(strings = ["Draw (São Paulo vs Away)", "Draw", "Tie", " draw (São Paulo vs Away) "])
    fun `GIVEN three team win markets WHEN mapped THEN only the iOS draw prefix forms a sports card`(draw: String) {
        // Arrange
        val clicks = mutableListOf<Triple<String, String, String>>()
        val event = createEvent().copy(
            totalMarketsCount = 3,
            markets = listOf("Away", draw, "São Paulo").mapIndexed { index, label ->
                createPolymarketMarket(
                    id = label, groupItemTitle = label,
                    outcomes = listOf(
                        createPolymarketOutcome(assetId = "$label-yes", title = " Yes ", probability = BigDecimal(index + 1).movePointLeft(1)),
                        createPolymarketOutcome(assetId = "$label-no", title = "No"),
                    ),
                )
            },
        )
        val converter = PolymarketEventUMConverter(
            {}, { eventId, marketId, assetId -> clicks += Triple(eventId, marketId, assetId) }, isPhaseTwoEnabled = true,
        )

        // Act
        val actual = converter.convert(event, createGame())
        actual.rows.flatMap { it.outcomes }.forEach { it.onClick() }

        // Assert
        if (draw.startsWith("Draw (")) {
            assertThat(actual.sports).isNotNull()
            assertThat(actual.rows.single().outcomes.map { it.title })
                .containsExactly(
                    stringReference("SAO1"), resourceReference(R.string.prediction_sports_draw), stringReference("AWY"),
                ).inOrder()
            assertThat(clicks).containsExactly(
                Triple("event-1", "São Paulo", "São Paulo-yes"),
                Triple("event-1", draw, "$draw-yes"),
                Triple("event-1", "Away", "Away-yes"),
            ).inOrder()
        } else {
            assertThat(actual.sports).isNull()
            assertThat(actual.rows).hasSize(3)
        }
    }

    private fun createEvent(homeLabel: String = "São Paulo") = createPolymarketEvent(
        markets = listOf(createPolymarketMarket(outcomes = listOf(
            createPolymarketOutcome(assetId = "away", title = "Away", probability = BigDecimal("0.4")),
            createPolymarketOutcome(assetId = "home", title = homeLabel, probability = BigDecimal("0.6")),
        ))),
    ).copy(gameId = "11", teams = listOf(away, home))

    private fun createGame() = PolymarketSportsState.Game(
        gameId = "11", score = PolymarketSportsState.Score(1, 2), period = "Q3", isLive = true,
        isEnded = false, isStale = false,
        home = PolymarketSportsState.Team(home, restIndex = 1),
        away = PolymarketSportsState.Team(away, restIndex = 0),
    )
}