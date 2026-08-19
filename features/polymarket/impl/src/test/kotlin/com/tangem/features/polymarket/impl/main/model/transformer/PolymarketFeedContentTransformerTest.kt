package com.tangem.features.polymarket.impl.main.model.transformer

import com.google.common.truth.Truth.assertThat
import com.tangem.domain.polymarket.model.PolymarketAccessMode
import com.tangem.domain.polymarket.model.PolymarketDisplayMode
import com.tangem.domain.polymarket.model.PolymarketEvent
import com.tangem.domain.polymarket.model.PolymarketEventsBatchListState
import com.tangem.domain.polymarket.model.PolymarketMarket
import com.tangem.domain.polymarket.model.PolymarketOutcome
import com.tangem.domain.polymarket.model.PolymarketStatus
import com.tangem.features.polymarket.impl.main.model.converter.PolymarketEventUMConverter
import com.tangem.features.polymarket.impl.main.ui.state.PolymarketCategoryTabUM
import com.tangem.features.polymarket.impl.main.ui.state.PolymarketMainUM
import com.tangem.pagination.Batch
import com.tangem.pagination.BatchFetchResult
import com.tangem.pagination.PaginationStatus
import com.tangem.test.core.ProvideTestModels
import kotlinx.collections.immutable.persistentListOf
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Nested
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.TestInstance
import org.junit.jupiter.params.ParameterizedTest

@TestInstance(TestInstance.Lifecycle.PER_CLASS)
internal class PolymarketFeedContentTransformerTest {

    private var reloadClicks = 0

    // The class is PER_CLASS, so the counter would leak between tests.
    @BeforeEach
    fun resetRecordedClicks() {
        reloadClicks = 0
    }

    private fun transform(state: PolymarketEventsBatchListState): PolymarketMainUM = PolymarketFeedContentTransformer(
        batchListState = state,
        eventUMConverter = PolymarketEventUMConverter(onEventClick = {}, onOutcomeClick = { _, _, _ -> }),
        onReloadClick = { reloadClicks++ },
    ).transform(prevState = PREV_STATE)

    @Nested
    @TestInstance(TestInstance.Lifecycle.PER_CLASS)
    inner class InitialLoading {

        @ParameterizedTest
        @ProvideTestModels
        fun transformInitialLoading(model: InitialLoadingModel) {
            // Act
            val actual = transform(PolymarketEventsBatchListState(data = emptyList(), status = model.status))

            // Assert
            assertThat(actual.content).isEqualTo(PolymarketMainUM.ContentUM.Loading)
        }

        private fun provideTestModels(): List<InitialLoadingModel> = listOf(
            InitialLoadingModel(status = PaginationStatus.None),
            InitialLoadingModel(status = PaginationStatus.InitialLoading),
        )
    }

    @Nested
    @TestInstance(TestInstance.Lifecycle.PER_CLASS)
    inner class FooterLoader {

        @ParameterizedTest
        @ProvideTestModels
        fun transformFooterLoader(model: FooterLoaderModel) {
            // Arrange
            val state = PolymarketEventsBatchListState(
                data = listOf(Batch(key = 0, data = listOf(createEvent()))),
                status = model.status,
            )

            // Act
            val actual = transform(state)

            // Assert
            assertThat((actual.content as PolymarketMainUM.ContentUM.Content).isLoadingNextPage)
                .isEqualTo(model.expected)
        }

        private fun provideTestModels(): List<FooterLoaderModel> = listOf(
            FooterLoaderModel(status = PaginationStatus.NextBatchLoading, expected = true),
            FooterLoaderModel(
                status = PaginationStatus.Paginating(
                    lastResult = BatchFetchResult.Success(data = listOf(createEvent()), empty = false, last = false),
                ),
                expected = false,
            ),
            FooterLoaderModel(status = PaginationStatus.EndOfPagination, expected = false),
        )
    }

    @Test
    fun `GIVEN loaded pages WHEN transform THEN their events are shown in order`() {
        // Arrange
        val state = PolymarketEventsBatchListState(
            data = listOf(
                Batch(key = 0, data = listOf(createEvent(id = "event-1"), createEvent(id = "event-2"))),
                Batch(key = 1, data = listOf(createEvent(id = "event-3"))),
            ),
            status = PaginationStatus.EndOfPagination,
        )

        // Act
        val actual = transform(state)

        // Assert
        assertThat((actual.content as PolymarketMainUM.ContentUM.Content).events.map { it.id })
            .containsExactly("event-1", "event-2", "event-3")
            .inOrder()
    }

    @Test
    fun `GIVEN the same event on two pages WHEN transform THEN it is shown once`() {
        // Arrange
        val state = PolymarketEventsBatchListState(
            data = listOf(
                Batch(key = 0, data = listOf(createEvent(id = "event-1"), createEvent(id = "event-2"))),
                Batch(key = 1, data = listOf(createEvent(id = "event-2"), createEvent(id = "event-3"))),
            ),
            status = PaginationStatus.EndOfPagination,
        )

        // Act
        val actual = transform(state)

        // Assert
        assertThat((actual.content as PolymarketMainUM.ContentUM.Content).events.map { it.id })
            .containsExactly("event-1", "event-2", "event-3")
            .inOrder()
    }

    @Test
    fun `GIVEN previous state WHEN transform THEN its tabs and access mode are kept`() {
        // Act
        val actual = transform(PolymarketEventsBatchListState(data = emptyList(), status = PaginationStatus.None))

        // Assert
        assertThat(actual.categories).isEqualTo(PREV_STATE.categories)
        assertThat(actual.accessMode).isEqualTo(PREV_STATE.accessMode)
    }

    @Test
    fun `GIVEN the first page failed WHEN transform THEN the reload prompt is shown`() {
        // Act
        val actual = transform(initialLoadingErrorState())

        // Assert
        assertThat(actual.content).isInstanceOf(PolymarketMainUM.ContentUM.Error::class.java)
    }

    @Test
    fun `GIVEN the reload prompt WHEN it is clicked THEN the reload is delegated`() {
        // Act
        (transform(initialLoadingErrorState()).content as PolymarketMainUM.ContentUM.Error).onReloadClick()

        // Assert
        assertThat(reloadClicks).isEqualTo(1)
    }

    internal data class InitialLoadingModel(val status: PaginationStatus<List<PolymarketEvent>>)

    internal data class FooterLoaderModel(
        val status: PaginationStatus<List<PolymarketEvent>>,
        val expected: Boolean,
    )

    private fun initialLoadingErrorState() = PolymarketEventsBatchListState(
        data = emptyList(),
        status = PaginationStatus.InitialLoadingError(throwable = IllegalStateException("boom")),
    )

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
        val PREV_STATE = PolymarketMainUM(
            accessMode = PolymarketAccessMode.TRADING,
            categories = persistentListOf(
                PolymarketCategoryTabUM(id = 1, label = "Politics", isSelected = true, onClick = {}),
            ),
            content = PolymarketMainUM.ContentUM.Loading,
        )
    }
}