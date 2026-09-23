package com.tangem.features.polymarket.impl.main.model.transformer

import com.google.common.truth.Truth.assertThat
import com.tangem.domain.polymarket.model.PolymarketAccessMode
import com.tangem.domain.polymarket.model.PolymarketDisplayMode
import com.tangem.domain.polymarket.model.PolymarketEvent
import com.tangem.domain.polymarket.model.PolymarketEventsBatch
import com.tangem.domain.polymarket.model.PolymarketEventsBatchListState
import com.tangem.domain.polymarket.model.PolymarketMarket
import com.tangem.domain.polymarket.model.PolymarketOutcome
import com.tangem.domain.polymarket.model.PolymarketStatus
import com.tangem.features.polymarket.impl.common.funding.RequisitesState
import com.tangem.features.polymarket.impl.main.model.PredictionBalanceMode
import com.tangem.features.polymarket.impl.main.model.converter.PolymarketEventUMConverter
import com.tangem.features.polymarket.impl.main.ui.state.PolymarketCategoryTabUM
import com.tangem.features.polymarket.impl.main.ui.state.PolymarketFeedFooterUM
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
import com.tangem.core.ui.components.containers.pullToRefresh.PullToRefreshConfig
import com.tangem.domain.models.account.PredictionAccountStatusValue
import com.tangem.features.polymarket.impl.main.model.converter.PredictionBalanceUMConverter
import kotlin.reflect.KClass

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

    private fun transform(state: PolymarketEventsBatchListState): PolymarketMainUM = PolymarketFeedContentTransformer(
        batchListState = state,
        eventUMConverter = PolymarketEventUMConverter(onEventClick = {}, onOutcomeClick = { _, _, _ -> }),
        onReloadClick = { reloadClicks++ },
        onNextPageRetryClick = { nextPageRetryClicks++ },
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
            assertThat((actual.content as PolymarketMainUM.ContentUM.Content).footer)
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
            val footer = (transform(state).content as PolymarketMainUM.ContentUM.Content).footer
            (footer as PolymarketFeedFooterUM.Error).onRetryClick()

            // Assert
            assertThat(nextPageRetryClicks).isEqualTo(1)
        }

        private fun provideTestModels(): List<FooterModel> = listOf(
            FooterModel(status = PaginationStatus.NextBatchLoading, expected = PolymarketFeedFooterUM.Loading::class),
            FooterModel(
                status = PaginationStatus.Paginating(
                    lastResult = BatchFetchResult.Success(data = batchOf(createEvent()), empty = false, last = false),
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
        assertThat((actual.content as PolymarketMainUM.ContentUM.Content).events.map { it.id })
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

    internal data class InitialLoadingModel(val status: PaginationStatus<PolymarketEventsBatch>)

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
                conditionId = null,
                eventId = id,
                title = "Market question",
                slug = "market-slug",
                description = "Market description",
                groupItemTitle = null,
                iconUrl = null,
                imageUrl = null,
                status = PolymarketStatus.ACTIVE,
                isNegRisk = false,
                isAcceptingOrders = true,
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
            balance = PredictionBalanceUMConverter(onAddFundsClick = {}, onWithdrawClick = {}, onBalanceModeClick = {})
                .convert(
                    PredictionBalanceUMConverter.Input(
                        status = PredictionAccountStatusValue.Loading,
                        requisites = RequisitesState.Loading,
                        isTradingAllowed = false,
                        mode = PredictionBalanceMode.AVAILABLE,
                    ),
                ),
            categories = persistentListOf(
                PolymarketCategoryTabUM(id = 1, label = "Politics", isSelected = true, onClick = {}),
            ),
            content = PolymarketMainUM.ContentUM.Loading,
            pullToRefreshConfig = PullToRefreshConfig(isRefreshing = false, onRefresh = {}),
        )
    }
}