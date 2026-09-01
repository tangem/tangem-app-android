package com.tangem.features.polymarket.impl.search.model.transformer

import com.google.common.truth.Truth.assertThat
import com.tangem.domain.polymarket.model.PolymarketDisplayMode
import com.tangem.domain.polymarket.model.PolymarketEvent
import com.tangem.domain.polymarket.model.PolymarketSearchBatchListState
import com.tangem.domain.polymarket.model.PolymarketMarket
import com.tangem.domain.polymarket.model.PolymarketOutcome
import com.tangem.domain.polymarket.model.PolymarketStatus
import com.tangem.features.polymarket.impl.main.model.converter.PolymarketEventUMConverter
import com.tangem.features.polymarket.impl.search.ui.state.PolymarketSearchUM
import com.tangem.pagination.Batch
import com.tangem.pagination.PaginationStatus
import com.tangem.test.core.ProvideTestModels
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Nested
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.TestInstance
import org.junit.jupiter.params.ParameterizedTest

@TestInstance(TestInstance.Lifecycle.PER_CLASS)
internal class PolymarketSearchContentTransformerTest {

    private var reloadClicks = 0

    // The class is PER_CLASS, so the counter would leak between tests.
    @BeforeEach
    fun resetRecordedClicks() {
        reloadClicks = 0
    }

    private fun transform(
        state: PolymarketSearchBatchListState,
        isQueryActive: Boolean = true,
    ): PolymarketSearchUM = PolymarketSearchContentTransformer(
        batchListState = state,
        isQueryActive = isQueryActive,
        eventUMConverter = PolymarketEventUMConverter(onEventClick = {}, onOutcomeClick = { _, _, _ -> }),
        onReloadClick = { reloadClicks++ },
    ).transform(prevState = PREV_STATE)

    @Test
    fun `GIVEN an inactive query WHEN transform THEN the prompt hides whatever the pagination holds`() {
        // Arrange
        val state = PolymarketSearchBatchListState(
            data = listOf(Batch(key = 0, data = listOf(createEvent()))),
            status = PaginationStatus.EndOfPagination,
        )

        // Act
        val actual = transform(state, isQueryActive = false)

        // Assert
        assertThat(actual.content).isEqualTo(PolymarketSearchUM.ContentUM.Initial)
    }

    @Nested
    @TestInstance(TestInstance.Lifecycle.PER_CLASS)
    inner class LoadingStates {

        @ParameterizedTest
        @ProvideTestModels
        fun transformLoading(model: LoadingModel) {
            // Act
            val actual = transform(PolymarketSearchBatchListState(data = emptyList(), status = model.status))

            // Assert
            assertThat(actual.content).isEqualTo(PolymarketSearchUM.ContentUM.Loading)
        }

        private fun provideTestModels(): List<LoadingModel> = listOf(
            LoadingModel(status = PaginationStatus.None),
            LoadingModel(status = PaginationStatus.InitialLoading),
        )
    }

    @Test
    fun `GIVEN loaded pages WHEN transform THEN their events are shown in order`() {
        // Arrange
        val state = PolymarketSearchBatchListState(
            data = listOf(
                Batch(key = 0, data = listOf(createEvent(id = "event-1"), createEvent(id = "event-2"))),
                Batch(key = 1, data = listOf(createEvent(id = "event-3"))),
            ),
            status = PaginationStatus.EndOfPagination,
        )

        // Act
        val actual = transform(state)

        // Assert
        assertThat((actual.content as PolymarketSearchUM.ContentUM.Results).events.map { it.id })
            .containsExactly("event-1", "event-2", "event-3")
            .inOrder()
    }

    @Test
    fun `GIVEN the same event on two pages WHEN transform THEN it is shown once`() {
        // Arrange
        val state = PolymarketSearchBatchListState(
            data = listOf(
                Batch(key = 0, data = listOf(createEvent(id = "event-1"), createEvent(id = "event-2"))),
                Batch(key = 1, data = listOf(createEvent(id = "event-2"), createEvent(id = "event-3"))),
            ),
            status = PaginationStatus.EndOfPagination,
        )

        // Act
        val actual = transform(state)

        // Assert
        assertThat((actual.content as PolymarketSearchUM.ContentUM.Results).events.map { it.id })
            .containsExactly("event-1", "event-2", "event-3")
            .inOrder()
    }

    @Test
    fun `GIVEN the next page on its way WHEN transform THEN the footer loader is shown`() {
        // Arrange
        val state = PolymarketSearchBatchListState(
            data = listOf(Batch(key = 0, data = listOf(createEvent()))),
            status = PaginationStatus.NextBatchLoading,
        )

        // Act
        val actual = transform(state)

        // Assert
        assertThat((actual.content as PolymarketSearchUM.ContentUM.Results).isLoadingNextPage).isTrue()
    }

    @Test
    fun `GIVEN nothing matched WHEN transform THEN the nothing-found prompt is shown`() {
        // Act
        val actual = transform(
            PolymarketSearchBatchListState(data = emptyList(), status = PaginationStatus.EndOfPagination),
        )

        // Assert
        assertThat(actual.content).isEqualTo(PolymarketSearchUM.ContentUM.NothingFound)
    }

    @Test
    fun `GIVEN a failed first page WHEN the prompt is clicked THEN the reload is delegated`() {
        // Arrange
        val state = PolymarketSearchBatchListState(
            data = emptyList(),
            status = PaginationStatus.InitialLoadingError(throwable = IllegalStateException("boom")),
        )

        // Act
        (transform(state).content as PolymarketSearchUM.ContentUM.Error).onReloadClick()

        // Assert
        assertThat(reloadClicks).isEqualTo(1)
    }

    @Test
    fun `GIVEN previous state WHEN transform THEN its query is kept`() {
        // Act
        val actual = transform(PolymarketSearchBatchListState(data = emptyList(), status = PaginationStatus.None))

        // Assert
        assertThat(actual.query).isEqualTo(PREV_STATE.query)
    }

    internal data class LoadingModel(val status: PaginationStatus<List<PolymarketEvent>>)

    private fun createEvent(id: String = "event-1"): PolymarketEvent = PolymarketEvent(
        id = id,
        slug = "$id-slug",
        title = "Event title",
        description = "Event description",
        rulesUrl = "https://polymarket.com/rules",
        iconUrl = null,
        imageUrl = null,
        status = PolymarketStatus.ACTIVE,
        startDate = null,
        endDate = null,
        volume = null,
        volume24h = null,
        liquidity = null,
        totalMarketsCount = 1,
        isNegRisk = false,
        displayMode = PolymarketDisplayMode.PLAIN_MARKETS,
        markets = listOf(
            PolymarketMarket(
                id = "market-1",
                eventId = id,
                title = "Market question",
                slug = "market-slug",
                description = "Market description",
                groupItemTitle = null,
                iconUrl = null,
                imageUrl = null,
                status = PolymarketStatus.ACTIVE,
                isNegRisk = false,
                startDate = null,
                endDate = null,
                startDateIso = null,
                endDateIso = null,
                volume = null,
                volume24h = null,
                liquidity = null,
                orderIndex = 0,
                outcomes = listOf(PolymarketOutcome(assetId = "asset-1", title = "Yes", probability = null)),
            ),
        ),
    )

    private companion object {
        val PREV_STATE = PolymarketSearchUM(
            query = "uzb",
            onQueryChange = {},
            onCloseClick = {},
            content = PolymarketSearchUM.ContentUM.Loading,
        )
    }
}