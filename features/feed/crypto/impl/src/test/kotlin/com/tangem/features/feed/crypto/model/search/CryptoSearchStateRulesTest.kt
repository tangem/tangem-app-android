package com.tangem.features.feed.crypto.model.search

import com.google.common.truth.Truth.assertThat
import com.tangem.core.ui.ds2.tokenicon.TangemTokenIcon
import com.tangem.core.ui.ds2.tokenrow.TangemTokenRowMarket
import com.tangem.core.ui.ds2.util.TangemPriceChange
import com.tangem.core.ui.extensions.stringReference
import com.tangem.common.ui.charts.state.MarketChartLook
import com.tangem.features.feed.crypto.ui.state.CryptoSearchUM
import com.tangem.features.feed.crypto.ui.state.MarketPulseItemUM
import com.tangem.features.feed.crypto.ui.state.MarketSearchUM
import com.tangem.features.feed.crypto.ui.state.PortfolioSearchUM
import kotlinx.collections.immutable.persistentListOf
import kotlinx.collections.immutable.toImmutableList
import org.junit.jupiter.api.Test

internal class CryptoSearchStateRulesTest {

    @Test
    fun `GIVEN market content WHEN starting a new query THEN results stay on screen`() {
        // Arrange
        val state = state(market = content(item("btc")))

        // Act
        val actual = state.startingNewQuery(query = "btcx")

        // Assert
        assertThat(actual.market).isEqualTo(state.market)
    }

    @Test
    fun `GIVEN any market state WHEN starting a new query THEN the query is carried into the state`() {
        // Arrange
        val state = state(market = content(item("btc")))

        // Act
        val actual = state.startingNewQuery(query = "btcx")

        // Assert
        assertThat(actual.query).isEqualTo("btcx")
    }

    @Test
    fun `GIVEN market not found WHEN starting a new query THEN market goes back to loading`() {
        // Arrange
        val state = state(market = MarketSearchUM.NotFound)

        // Act
        val actual = state.startingNewQuery(query = "btcx")

        // Assert
        assertThat(actual.market).isEqualTo(MarketSearchUM.Loading)
    }

    @Test
    fun `GIVEN market content WHEN a transient loading snapshot arrives THEN state is unchanged`() {
        // Arrange
        val state = state(market = content(item("btc")))

        // Act
        val actual = state.applyMarketSnapshot(MarketSearchUM.Loading)

        // Assert
        assertThat(actual).isEqualTo(state)
    }

    @Test
    fun `GIVEN market errored WHEN a retry starts loading THEN the shimmer replaces the error`() {
        // Arrange
        val state = state(market = MarketSearchUM.Error(onRetry = {}))

        // Act
        val actual = state.applyMarketSnapshot(MarketSearchUM.Loading)

        // Assert
        assertThat(actual.market).isEqualTo(MarketSearchUM.Loading)
    }

    @Test
    fun `GIVEN market not found WHEN a new search starts loading THEN the shimmer replaces it`() {
        // Arrange
        val state = state(market = MarketSearchUM.NotFound)

        // Act
        val actual = state.applyMarketSnapshot(MarketSearchUM.Loading)

        // Assert
        assertThat(actual.market).isEqualTo(MarketSearchUM.Loading)
    }

    @Test
    fun `GIVEN market content WHEN the search really found nothing THEN market becomes not found`() {
        // Arrange
        val state = state(market = content(item("btc")))

        // Act
        val actual = state.applyMarketSnapshot(MarketSearchUM.NotFound)

        // Assert
        assertThat(actual.market).isEqualTo(MarketSearchUM.NotFound)
    }

    @Test
    fun `GIVEN market content WHEN the source errors THEN the error snapshot lands`() {
        // Arrange
        val state = state(market = content(item("btc")))
        val error = MarketSearchUM.Error(onRetry = {})

        // Act
        val actual = state.applyMarketSnapshot(error)

        // Assert
        assertThat(actual.market).isEqualTo(error)
    }

    @Test
    fun `GIVEN some items under the cap limit WHEN building content THEN they are hidden behind a notification`() {
        // Arrange
        val items = persistentListOf(item("btc"), item("shady", isUnderMarketCapLimit = true))

        // Act
        val actual = buildMarketContent(items, shouldShowAllTokens = false, onShowAllTokens = {}, loadMore = {})

        // Assert
        assertThat(actual.items.map { it.id }).containsExactly("btc")
        assertThat(actual.shouldShowUnderMarketCapLimitNotification).isTrue()
    }

    @Test
    fun `GIVEN no items under the cap limit WHEN building content THEN no notification is shown`() {
        // Arrange
        val items = persistentListOf(item("btc"), item("eth"))

        // Act
        val actual = buildMarketContent(items, shouldShowAllTokens = false, onShowAllTokens = {}, loadMore = {})

        // Assert
        assertThat(actual.items).hasSize(2)
        assertThat(actual.shouldShowUnderMarketCapLimitNotification).isFalse()
    }

    @Test
    fun `GIVEN the show-all flag WHEN building content THEN every item is present and the notification is gone`() {
        // Arrange
        val items = persistentListOf(item("btc"), item("shady", isUnderMarketCapLimit = true))

        // Act
        val actual = buildMarketContent(items, shouldShowAllTokens = true, onShowAllTokens = {}, loadMore = {})

        // Assert
        assertThat(actual.items.map { it.id }).containsExactly("btc", "shady").inOrder()
        assertThat(actual.shouldShowUnderMarketCapLimitNotification).isFalse()
    }

    private fun state(market: MarketSearchUM) = CryptoSearchUM(
        query = "btc",
        portfolio = PortfolioSearchUM.Empty,
        market = market,
    )

    private fun content(vararg items: MarketPulseItemUM) = MarketSearchUM.Content(
        items = items.toList().toImmutableList(),
        loadMore = {},
    )

    private fun item(id: String, isUnderMarketCapLimit: Boolean = false) = MarketPulseItemUM(
        row = TangemTokenRowMarket.State.Content(
            id = id,
            icon = TangemTokenIcon.UiState.Token(TangemTokenIcon.State(url = null)),
            title = stringReference(id),
            ticker = stringReference(id),
            price = stringReference("$1"),
            priceChange = TangemPriceChange.State(
                value = stringReference("0%"),
                direction = TangemPriceChange.Direction.Neutral,
            ),
            onClick = {},
        ),
        chartData = null,
        chartType = MarketChartLook.Type.Neutral,
        isUnderMarketCapLimit = isUnderMarketCapLimit,
    )
}