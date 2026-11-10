package com.tangem.features.polymarket.impl.common

import com.google.common.truth.Truth.assertThat
import com.tangem.core.ui.extensions.stringReference
import com.tangem.domain.polymarket.model.PolymarketSearchBatchListState
import com.tangem.domain.polymarket.model.PolymarketSportsState
import com.tangem.domain.polymarket.model.PolymarketTeam
import com.tangem.features.polymarket.impl.createPolymarketEvent
import com.tangem.features.polymarket.impl.createPolymarketMarket
import com.tangem.features.polymarket.impl.createPolymarketOutcome
import com.tangem.features.polymarket.impl.details.model.transformer.PolymarketEventDetailsContentTransformer
import com.tangem.features.polymarket.impl.details.ui.state.PolymarketEventDetailsUM
import com.tangem.features.polymarket.impl.common.feed.model.converter.PolymarketEventUMConverter
import com.tangem.features.polymarket.impl.search.model.transformer.PolymarketSearchContentTransformer
import com.tangem.features.polymarket.impl.search.ui.state.PolymarketSearchUM
import com.tangem.pagination.Batch
import com.tangem.pagination.PaginationStatus
import org.junit.jupiter.params.ParameterizedTest
import org.junit.jupiter.params.provider.ValueSource
import java.math.BigDecimal

internal class PolymarketTeamButtonIntegrationTest {

    private val home = PolymarketTeam("Warriors", "gsw", null, null, "home")
    private val away = PolymarketTeam("Detroit Lions", "det", "Lions", null, "away")
    private val event = createPolymarketEvent(
        markets = listOf(
            createPolymarketMarket(
                outcomes = listOf(
                    createPolymarketOutcome(assetId = "away", title = "Lions", probability = BigDecimal("0.4")),
                    createPolymarketOutcome(assetId = "home", title = "Warriors", probability = BigDecimal("0.6")),
                ),
            ),
        ),
    ).copy(gameId = "11", teams = listOf(away, home))

    @ParameterizedTest
    @ValueSource(booleans = [true, false])
    fun `GIVEN sports metadata WHEN feed search and details render THEN labels follow toggle and trades retain identity`(
        enabled: Boolean,
    ) {
        // Arrange
        val clicks = mutableListOf<Triple<String, String, String>>()
        val converter = PolymarketEventUMConverter(
            onEventClick = {},
            onOutcomeClick = { event, market, asset -> clicks += Triple(event, market, asset) },
            isPhaseTwoEnabled = enabled,
        )
        val expected = if (enabled) listOf("DET", "GSW") else listOf("Lions", "Warriors")
        val previous = PolymarketSearchUM(
            query = "match", onQueryChange = {}, onCloseClick = {}, content = PolymarketSearchUM.ContentUM.Initial,
        )

        // Act
        val feed = converter.convert(event).rows.single().outcomes
        val search = PolymarketSearchContentTransformer(
            batchListState = PolymarketSearchBatchListState(
                data = listOf(Batch(0, listOf(event))), status = PaginationStatus.EndOfPagination,
            ),
            isQueryActive = true, eventUMConverter = converter, onReloadClick = {}, onNextPageRetryClick = {},
        ).transform(previous).content as PolymarketSearchUM.ContentUM.Results
        val searchButtons = search.events.single().rows.single().outcomes
        val details = PolymarketEventDetailsContentTransformer(
            event = event, isClosedMarketsExpanded = false, isDescriptionExpanded = false,
            onShareClick = {}, onOutcomeClick = { market, asset -> clicks += Triple(event.id, market, asset) },
            onClosedMarketsClick = {}, onReadMoreClick = {}, isTeamAbbreviationEnabled = enabled,
        ).transform(PolymarketEventDetailsUM.Loading) as PolymarketEventDetailsUM.Content
        val detailButtons = details.activeMarkets.single().outcomes
        feed.first().onClick()
        searchButtons.first().onClick()
        detailButtons.first().onClick()

        // Assert
        assertThat(feed.map { it.title }).containsExactlyElementsIn(expected.map(::stringReference)).inOrder()
        assertThat(searchButtons.map { it.title }).isEqualTo(feed.map { it.title })
        assertThat(detailButtons.map { it.title }).isEqualTo(feed.map { it.title })
        assertThat(feed.map { it.assetId }).containsExactly("away", "home").inOrder()
        assertThat(feed.map { it.price }).containsExactly(stringReference("40¢"), stringReference("60¢")).inOrder()
        assertThat(detailButtons.map { it.price }).isEqualTo(feed.map { it.price })
        assertThat(clicks).containsExactlyElementsIn(List(3) { Triple(event.id, "market-1", "away") })
    }

    @ParameterizedTest
    @ValueSource(booleans = [true, false])
    fun `GIVEN live sports state WHEN mapping THEN phase two gates layout labels and reordering`(enabled: Boolean) {
        // Arrange
        val clicks = mutableListOf<String>()
        val converter = PolymarketEventUMConverter(
            onEventClick = {}, onOutcomeClick = { _, _, asset -> clicks += asset },
            isPhaseTwoEnabled = enabled,
        )
        val game = PolymarketSportsState.Game(
            gameId = "11", score = PolymarketSportsState.Score(2, 1), period = "Q3", isLive = true, isEnded = false,
            home = PolymarketSportsState.Team(home, 1), away = PolymarketSportsState.Team(away, 0), isStale = false,
        )

        // Act
        val card = converter.convert(event, game)
        val buttons = card.rows.single().outcomes
        buttons.forEach { it.onClick() }

        // Assert
        val expected = if (enabled) listOf("GSW", "DET") else listOf("Lions", "Warriors")
        assertThat(buttons.map { it.title }).containsExactlyElementsIn(expected.map(::stringReference)).inOrder()
        val prices = if (enabled) listOf("60¢", "40¢") else listOf("40¢", "60¢")
        val assets = if (enabled) listOf("home", "away") else listOf("away", "home")
        assertThat(buttons.map { it.price }).containsExactlyElementsIn(prices.map(::stringReference)).inOrder()
        assertThat(buttons.map { it.assetId }).containsExactlyElementsIn(assets).inOrder()
        assertThat(clicks).containsExactlyElementsIn(assets).inOrder()
        if (enabled) {
            assertThat(card.sports?.home?.name).isEqualTo(stringReference("Warriors"))
            assertThat(card.sports?.away?.name).isEqualTo(stringReference("Detroit Lions"))
        } else {
            assertThat(card.sports).isNull()
            val restOnly = converter.convert(event)
            assertThat(card.rows.map { it.title }).isEqualTo(restOnly.rows.map { it.title })
            assertThat(buttons.map { it.accent }).isEqualTo(restOnly.rows.single().outcomes.map { it.accent })
        }
    }
}