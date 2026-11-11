package com.tangem.features.polymarket.impl.category.model

import com.google.common.truth.Truth.assertThat
import com.tangem.core.ui.components.containers.pullToRefresh.PullToRefreshConfig
import com.tangem.core.decompose.model.MutableParamsContainer
import com.tangem.core.decompose.navigation.Router
import com.tangem.core.decompose.ui.UiMessageSender
import com.tangem.core.ui.ds2.tabnavigation.TangemTabItemUM
import com.tangem.domain.models.wallet.UserWalletId
import com.tangem.domain.polymarket.model.PolymarketCategory
import com.tangem.domain.polymarket.model.PolymarketCategoryFilter
import com.tangem.domain.polymarket.model.PolymarketEventsBatch
import com.tangem.domain.polymarket.model.PolymarketEventsBatchAction
import com.tangem.domain.polymarket.model.PolymarketEventsBatchFlow
import com.tangem.domain.polymarket.model.PolymarketEventsBatchingContext
import com.tangem.domain.polymarket.model.PolymarketEventsListConfig
import com.tangem.domain.polymarket.model.PolymarketEventsUpdateRequest
import com.tangem.domain.polymarket.model.PolymarketSportsState
import com.tangem.domain.polymarket.usecase.GetPolymarketEventsBatchFlowUseCase
import com.tangem.domain.polymarket.usecase.ObservePolymarketSportsStateUseCase
import com.tangem.features.polymarket.impl.category.PolymarketCategoryComponent
import com.tangem.features.polymarket.impl.common.region.PredictionRegionAccess
import com.tangem.features.polymarket.impl.createPolymarketEvent
import com.tangem.features.polymarket.impl.common.feed.ui.state.PolymarketFeedFooterUM
import com.tangem.features.polymarket.impl.navigation.PolymarketRoute
import com.tangem.pagination.Batch
import com.tangem.pagination.BatchAction
import com.tangem.pagination.BatchListState
import com.tangem.pagination.BatchUpdateResult
import com.tangem.pagination.PaginationStatus
import com.tangem.test.core.createTestingCoroutineDispatcherProvider
import io.mockk.coEvery
import io.mockk.every
import io.mockk.mockk
import io.mockk.slot
import io.mockk.verify
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.Job
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.flowOf
import kotlinx.coroutines.flow.launchIn
import kotlinx.coroutines.flow.onEach
import kotlinx.coroutines.test.TestScope
import kotlinx.coroutines.test.runCurrent
import kotlinx.coroutines.test.runTest
import org.junit.jupiter.api.AfterEach
import org.junit.jupiter.api.Test
import com.tangem.features.polymarket.impl.common.feed.ui.state.PolymarketFeedContentUM

@OptIn(ExperimentalCoroutinesApi::class)
internal class PolymarketCategoryModelTest {

    private val router: Router = mockk(relaxed = true)
    private val messages: UiMessageSender = mockk(relaxed = true)
    private val region: PredictionRegionAccess = mockk {
        every { isRestricted } returns false
        coEvery { refresh() } returns Unit
    }
    private val getEvents: GetPolymarketEventsBatchFlowUseCase = mockk()
    private val observeSports: ObservePolymarketSportsStateUseCase = mockk {
        every { this@mockk.invoke(any(), any()) } returns flowOf(PolymarketSportsState(emptyMap()))
    }
    private val feeds = mutableListOf<Feed>()
    private var model: PolymarketCategoryModel? = null
    private val walletId = UserWalletId("011")
    private val games = PolymarketCategoryFilter.Games
    private val props = PolymarketCategoryFilter.Props

    @AfterEach
    fun destroy() {
        model?.onDestroy()
    }

    @Test
    fun `GIVEN server filter order and unknown filter WHEN opened THEN first known filter is selected`() = runTest {
        // Arrange
        val parent = category().copy(filters = listOf(PolymarketCategoryFilter.Unknown("future"), props, games))

        // Act
        val model = createModel(parent = parent)
        runCurrent()

        // Assert
        assertThat(model.uiState.value.selection).isEqualTo(PolymarketEventsListConfig(6, props))
        assertThat(model.uiState.value.filters.map { it.id }).containsExactly("props", "games").inOrder()
        assertThat(model.uiState.value.subcategories.single { it.isSelected }.id).isEqualTo(6)
        assertThat(feeds.single().actions).containsExactly(BatchAction.Reload(PolymarketEventsListConfig(6, props)))
    }

    @Test
    fun `GIVEN Questions selected WHEN child changes THEN filter resets or disappears according to child filters`() = runTest {
        // Arrange
        val model = createModel()
        runCurrent()
        model.clickFilter("props")
        runCurrent()

        // Act
        model.uiState.value.subcategories.single { it.id == 14 }.onClick()
        runCurrent()
        assertThat(model.uiState.value.selection).isEqualTo(PolymarketEventsListConfig(14, games))
        model.uiState.value.subcategories.single { it.id == 16 }.onClick()
        runCurrent()

        // Assert
        assertThat(model.uiState.value.filters).isEmpty()
        assertThat(model.uiState.value.selection).isEqualTo(PolymarketEventsListConfig(16))
        assertThat(feeds.last().actions).containsExactly(BatchAction.Reload(PolymarketEventsListConfig(16)))
    }

    @Test
    fun `GIVEN visited filters WHEN switching back THEN page one reloads and repeated tap is ignored`() = runTest {
        // Arrange
        val model = createModel()
        runCurrent()
        model.clickFilter("props")
        runCurrent()

        // Act
        model.clickFilter("games")
        runCurrent()
        model.clickFilter("games")
        runCurrent()

        // Assert
        assertThat(feeds).hasSize(3)
        assertThat(feeds.map { it.scope.coroutineContext[Job]?.isActive }).containsExactly(false, false, true).inOrder()
        assertThat(feeds.last().actions).containsExactly(BatchAction.Reload(PolymarketEventsListConfig(6, games)))
    }

    @Test
    fun `GIVEN loaded child event WHEN event and outcome tapped THEN routes preserve wallet and selected category`() = runTest {
        // Arrange
        val model = createModel()
        runCurrent()
        model.uiState.value.subcategories.single { it.id == 14 }.onClick()
        runCurrent()
        feeds.last().load("soccer")
        runCurrent()
        val card = model.cards().single()
        val event = createPolymarketEvent(id = "soccer")
        val market = event.markets.first()

        // Act
        card.onClick()
        card.rows.first().outcomes.first().onClick()

        // Assert
        verify {
            router.push(PolymarketRoute.EventDetails("soccer", walletId, categoryLabel = "soccer"))
            router.push(PolymarketRoute.PlacePrediction(walletId, "soccer", market.id, market.outcomes.first().assetId))
        }
    }

    @Test
    fun `GIVEN restricted region WHEN outcome tapped THEN warning replaces order navigation`() = runTest {
        // Arrange
        every { region.isRestricted } returns true
        val model = createModel()
        runCurrent()
        feeds.single().load("match")
        runCurrent()

        // Act
        model.cards().single().rows.first().outcomes.first().onClick()

        // Assert
        verify(exactly = 1) { messages.send(any()) }
        verify(exactly = 0) { router.push(any<PolymarketRoute.PlacePrediction>()) }
    }

    @Test
    fun `GIVEN loaded cards WHEN refreshing THEN old cards stay until the new first page replaces them`() = runTest {
        // Arrange
        val model = createModel()
        runCurrent()
        feeds.single().load("old")
        runCurrent()

        // Act
        model.uiState.value.pullToRefreshConfig.onRefresh(PullToRefreshConfig.ShowRefreshState())
        runCurrent()
        assertThat(model.cards().map { it.id }).containsExactly("old")
        assertThat(model.uiState.value.pullToRefreshConfig.isRefreshing).isTrue()
        feeds.last().load("new")
        runCurrent()

        // Assert
        assertThat(model.cards().map { it.id }).containsExactly("new")
        assertThat(model.uiState.value.pullToRefreshConfig.isRefreshing).isFalse()
        assertThat(feeds.first().scope.coroutineContext[Job]?.isActive).isFalse()
    }

    @Test
    fun `GIVEN refresh failure WHEN request ends THEN existing cards stay with a retry footer`() = runTest {
        // Arrange
        val model = createModel()
        runCurrent()
        feeds.single().load("old")
        runCurrent()
        model.uiState.value.pullToRefreshConfig.onRefresh(PullToRefreshConfig.ShowRefreshState())
        runCurrent()

        // Act
        feeds.last().state.value = BatchListState(
            emptyList(), PaginationStatus.InitialLoadingError(IllegalStateException("offline")),
        )
        runCurrent()

        // Assert
        assertThat(model.cards().map { it.id }).containsExactly("old")
        assertThat((model.uiState.value.content as PolymarketFeedContentUM.Content).footer)
            .isInstanceOf(PolymarketFeedFooterUM.Error::class.java)
        assertThat(model.uiState.value.pullToRefreshConfig.isRefreshing).isFalse()
        assertThat(feeds.last().scope.coroutineContext[Job]?.isActive).isFalse()
    }

    @Test
    fun `GIVEN pending refresh WHEN filter changes THEN late old response cannot replace the selected feed`() = runTest {
        // Arrange
        val model = createModel()
        runCurrent()
        feeds.single().load("old")
        runCurrent()
        model.uiState.value.pullToRefreshConfig.onRefresh(PullToRefreshConfig.ShowRefreshState())
        runCurrent()
        val refresh = feeds.last()

        // Act
        model.clickFilter("props")
        runCurrent()
        refresh.load("late-games")
        feeds.last().load("question")
        runCurrent()

        // Assert
        assertThat(model.cards().map { it.id }).containsExactly("question")
        assertThat(model.uiState.value.selection.filter).isEqualTo(props)
        assertThat(refresh.scope.coroutineContext[Job]?.isActive).isFalse()
        assertThat(model.uiState.value.pullToRefreshConfig.isRefreshing).isFalse()
    }

    @Test
    fun `GIVEN category lifecycle WHEN resumed paused and destroyed THEN live lifecycle and feed scope follow it`() = runTest {
        // Arrange
        val resumed = slot<Flow<Boolean>>()
        every { observeSports(any(), capture(resumed)) } returns flowOf(PolymarketSportsState(emptyMap()))
        val model = createModel()
        runCurrent()
        val states = mutableListOf<Boolean>()
        resumed.captured.onEach { states += it }.launchIn(backgroundScope)
        runCurrent()

        // Act
        model.onResume()
        runCurrent()
        model.onPause()
        runCurrent()
        model.onDestroy()
        runCurrent()

        // Assert
        assertThat(states).containsExactly(false, true, false).inOrder()
        assertThat(feeds.single().scope.coroutineContext[Job]?.isActive).isFalse()
    }

    @Test
    fun `GIVEN refresh retry from footer WHEN filter changes THEN retry request scope is cancelled too`() = runTest {
        // Arrange
        val model = createModel()
        runCurrent()
        feeds.single().load("old")
        runCurrent()
        model.uiState.value.pullToRefreshConfig.onRefresh(PullToRefreshConfig.ShowRefreshState())
        runCurrent()
        feeds.last().state.value = BatchListState(
            emptyList(), PaginationStatus.InitialLoadingError(IllegalStateException("offline")),
        )
        runCurrent()
        val footer = (model.uiState.value.content as PolymarketFeedContentUM.Content).footer
        (footer as PolymarketFeedFooterUM.Error).onRetryClick()
        runCurrent()
        val retry = feeds.last()

        // Act
        model.clickFilter("props")
        runCurrent()
        feeds.last().load("question")
        runCurrent()

        // Assert
        assertThat(retry.scope.coroutineContext[Job]?.isActive).isFalse()
        assertThat(model.cards().map { it.id }).containsExactly("question")
    }

    private fun PolymarketCategoryModel.clickFilter(id: String) {
        (uiState.value.filters.single { it.id == id } as TangemTabItemUM.Content).onClick()
    }

    private fun PolymarketCategoryModel.cards() = (uiState.value.content as PolymarketFeedContentUM.Content).events

    private fun TestScope.createModel(
        parent: PolymarketCategory = category(),
    ): PolymarketCategoryModel {
        every { getEvents(any(), any(), any()) } answers {
            val context = secondArg<PolymarketEventsBatchingContext>()
            val feed = Feed(context.coroutineScope)
            feeds += feed
            context.actionsFlow.onEach { feed.actions += it }.launchIn(context.coroutineScope)
            feed.batches
        }
        return PolymarketCategoryModel(
            paramsContainer = MutableParamsContainer(PolymarketCategoryComponent.Params(walletId, parent)),
            router = router,
            messageSender = messages,
            regionAccess = region,
            getEvents = getEvents,
            observeSports = observeSports,
            observeQuotes = mockk(),
            dispatchers = createTestingCoroutineDispatcherProvider(),
        ).also { model = it }
    }

    private fun category() = PolymarketCategory(
        id = 6,
        label = "Sports",
        icon = null,
        filters = listOf(games, props),
        subcategories = listOf(
            PolymarketCategory(id = 14, label = "soccer", icon = null, filters = listOf(games, props)),
            PolymarketCategory(id = 16, label = "basketball", icon = null, filters = listOf(games)),
        ),
    )

    private class Feed(val scope: CoroutineScope) {
        val actions = mutableListOf<PolymarketEventsBatchAction>()
        val state = MutableStateFlow(
            BatchListState<Int, PolymarketEventsBatch>(emptyList(), PaginationStatus.InitialLoading),
        )
        val batches = object : PolymarketEventsBatchFlow {
            override val state = this@Feed.state
            override val updateResults =
                MutableSharedFlow<Pair<PolymarketEventsUpdateRequest, BatchUpdateResult<Int, PolymarketEventsBatch>>>()
        }

        fun load(id: String) {
            state.value = BatchListState(
                listOf(Batch(0, PolymarketEventsBatch(listOf(createPolymarketEvent(id = id)), requestCursor = null))),
                PaginationStatus.EndOfPagination,
            )
        }
    }
}