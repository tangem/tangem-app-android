package com.tangem.features.feed.rwa.model.search

import com.google.common.truth.Truth.assertThat
import com.tangem.core.ui.ds2.tokenicon.TangemTokenIcon
import com.tangem.core.ui.ds2.tokenrow.TangemTokenRowMarket
import com.tangem.core.ui.ds2.util.TangemPriceChange
import com.tangem.core.ui.extensions.stringReference
import com.tangem.common.ui.charts.state.MarketChartLook
import com.tangem.features.feed.rwa.ui.state.RwaListItemUM
import com.tangem.features.feed.rwa.ui.state.RwaSearchListUM
import com.tangem.features.feed.rwa.ui.state.RwaSearchPortfolioUM
import kotlinx.collections.immutable.persistentListOf
import org.junit.jupiter.api.Test

internal class RwaSearchStateRulesTest {

    @Test
    fun `GIVEN nothing searched yet WHEN the initial state is built THEN it shows a blank query and a shimmer`() {
        // Act
        val actual = initialRwaSearchState()

        // Assert
        assertThat(actual.query).isEmpty()
        assertThat(actual.portfolio).isEqualTo(RwaSearchPortfolioUM.Empty)
        assertThat(actual.list).isEqualTo(RwaSearchListUM.Loading)
    }

    @Test
    fun `GIVEN results on screen WHEN a new query starts THEN they are kept instead of flashing a shimmer`() {
        // Arrange
        val content = content()
        val state = initialRwaSearchState().copy(list = content)

        // Act
        val actual = state.startingNewQuery("tsla")

        // Assert
        assertThat(actual.query).isEqualTo("tsla")
        assertThat(actual.list).isSameInstanceAs(content)
    }

    @Test
    fun `GIVEN a not-found state WHEN a new query starts THEN the shimmer comes back`() {
        // Arrange
        val state = initialRwaSearchState().copy(list = RwaSearchListUM.NotFound)

        // Act
        val actual = state.startingNewQuery("tsla")

        // Assert
        assertThat(actual.list).isEqualTo(RwaSearchListUM.Loading)
    }

    @Test
    fun `GIVEN an error state WHEN a new query starts THEN the shimmer comes back`() {
        // Arrange
        val state = initialRwaSearchState().copy(list = RwaSearchListUM.Error(onRetry = {}))

        // Act
        val actual = state.startingNewQuery("tsla")

        // Assert
        assertThat(actual.list).isEqualTo(RwaSearchListUM.Loading)
    }

    @Test
    fun `GIVEN content on screen WHEN a loading snapshot arrives THEN it does not displace the content`() {
        // Arrange
        val state = initialRwaSearchState().copy(list = content())

        // Act
        val actual = state.applyListSnapshot(RwaSearchListUM.Loading)

        // Assert — a batch flow reports an empty list between a reload and its first batch
        assertThat(actual).isSameInstanceAs(state)
    }

    @Test
    fun `GIVEN a not-found state WHEN a loading snapshot arrives THEN the shimmer replaces it`() {
        // Arrange
        val state = initialRwaSearchState().copy(list = RwaSearchListUM.NotFound)

        // Act
        val actual = state.applyListSnapshot(RwaSearchListUM.Loading)

        // Assert — this is how a retry gets its shimmer back
        assertThat(actual.list).isEqualTo(RwaSearchListUM.Loading)
    }

    @Test
    fun `GIVEN an error state WHEN a loading snapshot arrives THEN the shimmer replaces it`() {
        // Arrange
        val state = initialRwaSearchState().copy(list = RwaSearchListUM.Error(onRetry = {}))

        // Act
        val actual = state.applyListSnapshot(RwaSearchListUM.Loading)

        // Assert
        assertThat(actual.list).isEqualTo(RwaSearchListUM.Loading)
    }

    @Test
    fun `GIVEN content on screen WHEN a not-found snapshot arrives THEN it replaces the content`() {
        // Arrange
        val state = initialRwaSearchState().copy(list = content())

        // Act
        val actual = state.applyListSnapshot(RwaSearchListUM.NotFound)

        // Assert
        assertThat(actual.list).isEqualTo(RwaSearchListUM.NotFound)
    }

    @Test
    fun `GIVEN content on screen WHEN newer content arrives THEN it replaces the old content`() {
        // Arrange
        val state = initialRwaSearchState().copy(list = content())
        val newer = content(id = "aaplx")

        // Act
        val actual = state.applyListSnapshot(newer)

        // Assert
        assertThat(actual.list).isSameInstanceAs(newer)
    }

    private fun content(id: String = "tslax") = RwaSearchListUM.Content(
        items = persistentListOf(item(id)),
        loadMore = {},
    )

    private fun item(id: String) = RwaListItemUM(
        row = TangemTokenRowMarket.State.Content(
            id = id,
            icon = TangemTokenIcon.UiState.Token(TangemTokenIcon.State(url = "")),
            title = stringReference(id.uppercase()),
            ticker = stringReference(id),
            price = stringReference("$1.00"),
            priceChange = TangemPriceChange.State(
                value = stringReference("0%"),
                direction = TangemPriceChange.Direction.Neutral,
            ),
        ),
        chartData = null,
        chartType = MarketChartLook.Type.Growing,
    )
}