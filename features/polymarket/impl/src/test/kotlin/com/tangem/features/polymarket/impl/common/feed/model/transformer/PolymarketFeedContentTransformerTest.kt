package com.tangem.features.polymarket.impl.common.feed.model.transformer

import com.google.common.truth.Truth.assertThat
import com.tangem.domain.polymarket.model.PolymarketEvent
import com.tangem.domain.polymarket.model.PolymarketEventsBatch
import com.tangem.domain.polymarket.model.PolymarketEventsBatchListState
import com.tangem.features.polymarket.impl.createPolymarketEvent
import com.tangem.features.polymarket.impl.common.feed.model.converter.PolymarketEventUMConverter
import com.tangem.features.polymarket.impl.common.feed.ui.state.PolymarketFeedFooterUM
import com.tangem.pagination.Batch
import com.tangem.pagination.BatchFetchResult
import com.tangem.pagination.PaginationStatus
import com.tangem.test.core.ProvideTestModels
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Nested
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.TestInstance
import org.junit.jupiter.params.ParameterizedTest
import kotlin.reflect.KClass
import com.tangem.features.polymarket.impl.common.feed.ui.state.PolymarketFeedContentUM

@TestInstance(TestInstance.Lifecycle.PER_CLASS)
internal class PolymarketFeedContentTransformerTest {

    private var reloadClicks = 0
    private var nextPageRetryClicks = 0

    // The class is PER_CLASS, so the counters would leak between tests.
    @BeforeEach
    fun resetRecordedClicks() {
        reloadClicks = 0
        nextPageRetryClicks = 0
    }

    private fun transform(state: PolymarketEventsBatchListState): PolymarketFeedContentUM = PolymarketFeedContentTransformer(
        batchListState = state,
        eventUMConverter = PolymarketEventUMConverter(onEventClick = {}, onOutcomeClick = { _, _, _ -> }, isPhaseTwoEnabled = false),
        onReloadClick = { reloadClicks++ },
        onNextPageRetryClick = { nextPageRetryClicks++ },
    ).transformContent()

    @Nested
    @TestInstance(TestInstance.Lifecycle.PER_CLASS)
    inner class InitialLoading {

        @ParameterizedTest
        @ProvideTestModels
        fun transformInitialLoading(model: InitialLoadingModel) {
            // Act
            val actual = transform(PolymarketEventsBatchListState(data = emptyList(), status = model.status))

            // Assert
            assertThat(actual).isEqualTo(PolymarketFeedContentUM.Loading)
        }

        private fun provideTestModels(): List<InitialLoadingModel> = listOf(
            InitialLoadingModel(status = PaginationStatus.None),
            InitialLoadingModel(status = PaginationStatus.InitialLoading),
        )
    }

    @Nested
    @TestInstance(TestInstance.Lifecycle.PER_CLASS)
    inner class Footer {

        @ParameterizedTest
        @ProvideTestModels
        fun transformFooter(model: FooterModel) {
            // Arrange
            val state = PolymarketEventsBatchListState(
                data = listOf(Batch(key = 0, data = batchOf(createEvent()))),
                status = model.status,
            )

            // Act
            val actual = transform(state)

            // Assert
            assertThat((actual as PolymarketFeedContentUM.Content).footer)
                .isInstanceOf(model.expected.java)
        }

        @Test
        fun `GIVEN the next page failed WHEN the footer is clicked THEN the retry is delegated`() {
            // Arrange
            val state = PolymarketEventsBatchListState(
                data = listOf(Batch(key = 0, data = batchOf(createEvent()))),
                status = PaginationStatus.Paginating(lastResult = BatchFetchResult.Error(IllegalStateException("boom"))),
            )

            // Act
            val footer = (transform(state) as PolymarketFeedContentUM.Content).footer
            (footer as PolymarketFeedFooterUM.Error).onRetryClick()

            // Assert
            assertThat(nextPageRetryClicks).isEqualTo(1)
        }

        private fun provideTestModels(): List<FooterModel> = listOf(
            FooterModel(status = PaginationStatus.NextBatchLoading, expected = PolymarketFeedFooterUM.Loading::class),
            FooterModel(
                status = PaginationStatus.Paginating(
                    lastResult = BatchFetchResult.Success(data = batchOf(createEvent()), isEmpty = false, isLast = false),
                ),
                expected = PolymarketFeedFooterUM.None::class,
            ),
            FooterModel(
                status = PaginationStatus.Paginating(lastResult = BatchFetchResult.Error(IllegalStateException("boom"))),
                expected = PolymarketFeedFooterUM.Error::class,
            ),
            FooterModel(status = PaginationStatus.EndOfPagination, expected = PolymarketFeedFooterUM.None::class),
        )
    }

    @Nested
    @TestInstance(TestInstance.Lifecycle.PER_CLASS)
    inner class EmptyFeed {

        @ParameterizedTest
        @ProvideTestModels
        fun transformEmptyFeed(model: EmptyFeedModel) {
            // Act
            val actual = transform(PolymarketEventsBatchListState(data = emptyList(), status = model.status))

            // Assert
            assertThat(actual).isEqualTo(PolymarketFeedContentUM.Empty)
        }

        private fun provideTestModels(): List<EmptyFeedModel> = listOf(
            EmptyFeedModel(status = PaginationStatus.EndOfPagination),
            EmptyFeedModel(
                status = PaginationStatus.Paginating(
                    lastResult = BatchFetchResult.Success(data = batchOf(), isEmpty = true, isLast = false),
                ),
            ),
        )
    }

    @Test
    fun `GIVEN loaded pages WHEN transform THEN their events are shown in order`() {
        // Arrange
        val state = PolymarketEventsBatchListState(
            data = listOf(
                Batch(key = 0, data = batchOf(createEvent(id = "event-1"), createEvent(id = "event-2"))),
                Batch(key = 1, data = batchOf(createEvent(id = "event-3"))),
            ),
            status = PaginationStatus.EndOfPagination,
        )

        // Act
        val actual = transform(state)

        // Assert
        assertThat((actual as PolymarketFeedContentUM.Content).events.map { it.id })
            .containsExactly("event-1", "event-2", "event-3")
            .inOrder()
    }

    @Test
    fun `GIVEN the same event on two pages WHEN transform THEN it is shown once`() {
        // Arrange
        val state = PolymarketEventsBatchListState(
            data = listOf(
                Batch(key = 0, data = batchOf(createEvent(id = "event-1"), createEvent(id = "event-2"))),
                Batch(key = 1, data = batchOf(createEvent(id = "event-2"), createEvent(id = "event-3"))),
            ),
            status = PaginationStatus.EndOfPagination,
        )

        // Act
        val actual = transform(state)

        // Assert
        assertThat((actual as PolymarketFeedContentUM.Content).events.map { it.id })
            .containsExactly("event-1", "event-2", "event-3")
            .inOrder()
    }

    @Test
    fun `GIVEN the first page failed WHEN transform THEN the reload prompt is shown`() {
        // Act
        val actual = transform(initialLoadingErrorState())

        // Assert
        assertThat(actual).isInstanceOf(PolymarketFeedContentUM.Error::class.java)
    }

    @Test
    fun `GIVEN the reload prompt WHEN it is clicked THEN the reload is delegated`() {
        // Act
        (transform(initialLoadingErrorState()) as PolymarketFeedContentUM.Error).onReloadClick()

        // Assert
        assertThat(reloadClicks).isEqualTo(1)
    }

    internal data class InitialLoadingModel(val status: PaginationStatus<PolymarketEventsBatch>)

    internal data class EmptyFeedModel(val status: PaginationStatus<PolymarketEventsBatch>)

    internal data class FooterModel(
        val status: PaginationStatus<PolymarketEventsBatch>,
        val expected: KClass<out PolymarketFeedFooterUM>,
    )

    private fun initialLoadingErrorState() = PolymarketEventsBatchListState(
        data = emptyList(),
        status = PaginationStatus.InitialLoadingError(throwable = IllegalStateException("boom")),
    )

    private fun batchOf(vararg events: PolymarketEvent) = PolymarketEventsBatch(
        events = events.toList(),
        requestCursor = null,
    )

    private fun createEvent(id: String = "event-1"): PolymarketEvent = createPolymarketEvent(
        id = id,
        slug = "$id-slug",
    )
}