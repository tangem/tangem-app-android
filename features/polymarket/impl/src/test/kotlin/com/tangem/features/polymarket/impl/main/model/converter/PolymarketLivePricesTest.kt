package com.tangem.features.polymarket.impl.main.model.converter

import com.google.common.truth.Truth.assertThat
import com.tangem.core.ui.extensions.stringReference
import com.tangem.domain.polymarket.model.PolymarketDisplayMode
import com.tangem.domain.polymarket.model.PolymarketLiveQuote
import com.tangem.domain.polymarket.model.PolymarketSportsState
import com.tangem.domain.polymarket.model.PolymarketTeam
import com.tangem.features.polymarket.impl.common.probability
import com.tangem.features.polymarket.impl.createPolymarketEvent
import com.tangem.features.polymarket.impl.createPolymarketMarket
import com.tangem.features.polymarket.impl.createPolymarketOutcome
import com.tangem.utils.StringsSigns.DASH_SIGN
import org.junit.jupiter.api.Test
import org.junit.jupiter.params.ParameterizedTest
import org.junit.jupiter.params.provider.CsvSource
import org.junit.jupiter.params.provider.EnumSource
import java.math.BigDecimal

internal class PolymarketLivePricesTest {

    @ParameterizedTest
    @EnumSource(PolymarketLiveQuote.Status::class)
    fun `GIVEN a quote status WHEN mapping a live card THEN only healthy quotes replace REST`(status: PolymarketLiveQuote.Status) {
        // Arrange
        val event = event()
        val quote = quote(ask = "0.62").copy(status = status)

        // Act
        val actual = converter().convert(event, sports = null, quotes = mapOf("yes" to quote))

        // Assert
        val expected = if (status == PolymarketLiveQuote.Status.LIVE) "62¢" else "40¢"
        assertThat(actual.rows.single().outcomes.first().price).isEqualTo(stringReference(expected))
    }

    @Test
    fun `GIVEN an empty live ask WHEN mapping THEN a dash replaces the REST price`() {
        // Arrange
        val event = event()

        // Act
        val actual = converter().convert(event, sports = null, quotes = mapOf("yes" to quote(ask = null)))

        // Assert
        assertThat(actual.rows.single().outcomes.first().price).isEqualTo(stringReference(DASH_SIGN))
    }

    @ParameterizedTest
    @CsvSource("false,true,false", "true,false,false", "true,true,true")
    fun `GIVEN disabled D2 prematch or ended WHEN quotes exist THEN the REST price stays`(
        enabled: Boolean,
        live: Boolean,
        ended: Boolean,
    ) {
        // Arrange
        val event = event().copy(isLive = live, isEnded = ended)

        // Act
        val actual = converter(enabled).convert(event, sports = null, quotes = mapOf("yes" to quote(ask = "0.62")))

        // Assert
        assertThat(actual.rows.single().outcomes.first().price).isEqualTo(stringReference("40¢"))
    }

    @ParameterizedTest
    @CsvSource(
        "0.40,0.50,0.70,45%",
        "0.40,0.51,0.70,70%",
        "0.40,0.51,,40%",
        ",0.50,0.70,70%",
        "0.40,,0.70,70%",
        ",,,40%",
    )
    fun `GIVEN grouped markets WHEN mapping live probability THEN midpoint spread and trade fallbacks follow iOS`(
        bid: String?,
        ask: String?,
        trade: String?,
        expected: String,
    ) {
        // Arrange
        val event = event().copy(displayMode = PolymarketDisplayMode.GROUPED_OUTCOMES)
        val grouped = event.copy(markets = event.markets.map { it.copy(groupItemTitle = "Candidate") })
        val quote = quote(ask).copy(bestBid = bid?.toBigDecimal(), lastTradePrice = trade?.toBigDecimal())

        // Act
        val actual = converter().convert(grouped, sports = null, quotes = mapOf("yes" to quote))

        // Assert
        assertThat(actual.rows.single().probability).isEqualTo(stringReference(expected))
    }

    @Test
    fun `GIVEN an odd cent spread WHEN calculating midpoint THEN half a cent is kept until formatting`() {
        // Arrange
        val quote = quote("0.41").copy(bestBid = BigDecimal("0.40"))

        // Act
        val actual = quote.probability(restProbability = BigDecimal("0.30"))

        // Assert
        assertThat(actual).isEqualTo(BigDecimal("0.405"))
    }

    @Test
    fun `GIVEN healthy quotes WHEN REST refreshes metadata THEN price stays live and click targets are unchanged`() {
        // Arrange
        val clicks = mutableListOf<Triple<String, String, String>>()
        val converter = PolymarketEventUMConverter(
            {}, { event, market, asset -> clicks += Triple(event, market, asset) }, isPhaseTwoEnabled = true,
        )
        val event = event()
        val refreshed = event.copy(title = "New title", markets = event.markets.map { market ->
            market.copy(outcomes = market.outcomes.map { it.copy(probability = BigDecimal("0.20")) })
        })

        // Act
        val actual = converter.convert(refreshed, sports = null, quotes = mapOf("yes" to quote(ask = "0.62")))
        actual.rows.single().outcomes.first().onClick()

        // Assert
        assertThat(actual.title).isEqualTo(stringReference("New title"))
        assertThat(actual.rows.single().outcomes.first().price).isEqualTo(stringReference("62¢"))
        assertThat(clicks).containsExactly(Triple("event-1", "market-1", "yes"))
    }

    @Test
    fun `GIVEN three sports markets WHEN reordered THEN asks and purchases stay with home draw and away tokens`() {
        // Arrange
        val home = PolymarketTeam("Home", "HME", null, null, "home")
        val away = PolymarketTeam("Away", "AWY", null, null, "away")
        val event = event().copy(
            isLive = false, gameId = "1", teams = listOf(away, home), totalMarketsCount = 3,
            markets = listOf("Away", "Draw (Home vs Away)", "Home").map { label ->
                createPolymarketMarket(
                    id = label, groupItemTitle = label,
                    outcomes = listOf(
                        createPolymarketOutcome(assetId = "$label-yes", title = "Yes", probability = BigDecimal("0.2")),
                        createPolymarketOutcome(assetId = "$label-no", title = "No"),
                    ),
                )
            },
        )
        val game = PolymarketSportsState.Game(
            gameId = "1", score = PolymarketSportsState.Score(1, 2), period = "INT", isLive = true, isEnded = false,
            home = PolymarketSportsState.Team(home, 1), away = PolymarketSportsState.Team(away, 0), isStale = false,
        )
        val asks = mapOf("Home-yes" to "0.36", "Draw (Home vs Away)-yes" to "0.32", "Away-yes" to "0.34")
        val quotes = asks.mapValues { (id, ask) -> quote(ask).copy(assetId = id) }
        val clicks = mutableListOf<Triple<String, String, String>>()
        val converter = PolymarketEventUMConverter(
            {}, { id, market, asset -> clicks += Triple(id, market, asset) }, isPhaseTwoEnabled = true,
        )

        // Act
        val actual = converter.convert(event, game, quotes)
        actual.rows.single().outcomes.forEach { it.onClick() }
        val ended = converter.convert(event.copy(isLive = true), game.copy(isLive = false, isEnded = true), quotes)

        // Assert
        assertThat(actual.sports?.isLive).isFalse()
        assertThat(actual.rows.single().outcomes.map { it.price })
            .containsExactly(stringReference("36¢"), stringReference("32¢"), stringReference("34¢")).inOrder()
        assertThat(clicks).containsExactly(
            Triple("event-1", "Home", "Home-yes"),
            Triple("event-1", "Draw (Home vs Away)", "Draw (Home vs Away)-yes"),
            Triple("event-1", "Away", "Away-yes"),
        ).inOrder()
        assertThat(ended.rows.single().outcomes.map { it.price }).containsExactly(
            stringReference("20¢"), stringReference("20¢"), stringReference("20¢"),
        )
    }

    private fun event() = createPolymarketEvent(
        isLive = true,
        markets = listOf(createPolymarketMarket(outcomes = listOf(
            createPolymarketOutcome(assetId = "yes", title = "Yes", probability = BigDecimal("0.40")),
            createPolymarketOutcome(assetId = "no", title = "No", probability = BigDecimal("0.60")),
        ))),
    )

    private fun converter(enabled: Boolean = true) = PolymarketEventUMConverter({}, { _, _, _ -> }, isPhaseTwoEnabled = enabled)

    private fun quote(ask: String?) = PolymarketLiveQuote(
        assetId = "yes", bestAsk = ask?.toBigDecimal(), status = PolymarketLiveQuote.Status.LIVE,
    )
}