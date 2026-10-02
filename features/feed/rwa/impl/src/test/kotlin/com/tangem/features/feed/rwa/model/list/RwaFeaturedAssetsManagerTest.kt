package com.tangem.features.feed.rwa.model.list

import com.google.common.truth.Truth.assertThat
import com.tangem.core.ui.ds2.util.TangemPriceChange
import com.tangem.domain.appcurrency.model.AppCurrency
import com.tangem.domain.markets.GetMarketsTokenListFlowUseCase
import com.tangem.domain.markets.TokenListBatchFlow
import com.tangem.domain.markets.TokenListBatchingContext
import com.tangem.domain.markets.TokenMarket
import com.tangem.domain.markets.TokenMarketListConfig
import com.tangem.domain.markets.TokenMarketUpdateRequest
import com.tangem.domain.markets.TokenQuotesShort
import com.tangem.domain.models.currency.CryptoCurrency
import com.tangem.features.feed.rwa.ui.state.RwaFeaturedUM
import com.tangem.pagination.Batch
import com.tangem.pagination.BatchAction
import com.tangem.pagination.BatchListState
import com.tangem.pagination.BatchUpdateResult
import com.tangem.pagination.PaginationStatus
import io.mockk.CapturingSlot
import io.mockk.clearMocks
import io.mockk.every
import io.mockk.mockk
import io.mockk.slot
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.CoroutineStart
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.Job
import kotlinx.coroutines.cancel
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharedFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.launch
import kotlinx.coroutines.test.StandardTestDispatcher
import kotlinx.coroutines.test.TestScope
import kotlinx.coroutines.test.advanceUntilIdle
import kotlinx.coroutines.test.runTest
import org.junit.jupiter.api.AfterEach
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.TestInstance
import java.math.BigDecimal

@OptIn(ExperimentalCoroutinesApi::class)
@TestInstance(TestInstance.Lifecycle.PER_CLASS)
internal class RwaFeaturedAssetsManagerTest {

    private val getMarketsTokenListFlowUseCase: GetMarketsTokenListFlowUseCase = mockk()
    private val scopes = mutableListOf<CoroutineScope>()

    @AfterEach
    fun cancelScopes() {
        scopes.forEach(CoroutineScope::cancel)
        scopes.clear()
    }

    @Test
    fun `GIVEN a featured category WHEN load THEN the request asks for the curated order`() = runTest {
        // Arrange
        val actions = captureActions()
        val manager = createManager(testScope = this)
        advanceUntilIdle()

        // Act
        manager.load(categoryId = FEATURED_CATEGORY_ID, country = null)
        advanceUntilIdle()

        // Assert
        val config = reloadConfigs(actions).single()
        assertThat(config.order).isEqualTo(TokenMarketListConfig.Order.ByCategory)
        assertThat(config.categoryId).isEqualTo(FEATURED_CATEGORY_ID)
        assertThat(config.searchText).isNull()
        assertThat(config.sectorId).isNull()
    }

    @Test
    fun `GIVEN a known country WHEN load THEN the request carries it`() = runTest {
        // Arrange — the flag the rail marks its assets with is evaluated for this country alone
        val actions = captureActions()
        val manager = createManager(testScope = this)
        advanceUntilIdle()

        // Act
        manager.load(categoryId = FEATURED_CATEGORY_ID, country = "US")
        advanceUntilIdle()

        // Assert
        assertThat(reloadConfigs(actions).single().country).isEqualTo("US")
    }

    @Test
    fun `GIVEN a restricted asset WHEN the rail is mapped THEN it is marked`() = runTest {
        // Arrange
        val restricted = createToken(id = "restricted", isRestrictedForCountry = true)
        val allowed = createToken(id = "allowed")
        captureActions(state = MutableStateFlow(stateOf(restricted, allowed)))

        // Act
        val manager = createManager(testScope = this)
        advanceUntilIdle()

        // Assert
        val assets = (manager.state.value as RwaFeaturedUM.Content).assets
        assertThat(assets.single { it.id == "restricted" }.indicator).isNotNull()
        assertThat(assets.single { it.id == "allowed" }.indicator).isNull()
    }

    @Test
    fun `GIVEN the tab is on a week interval WHEN load THEN the request carries it`() = runTest {
        // Arrange
        val actions = captureActions()
        val manager = createManager(testScope = this, interval = MutableStateFlow(RwaInterval.D7))
        advanceUntilIdle()

        // Act
        manager.load(categoryId = FEATURED_CATEGORY_ID, country = null)
        advanceUntilIdle()

        // Assert
        assertThat(reloadConfigs(actions).single().priceChangeInterval)
            .isEqualTo(TokenMarketListConfig.Interval.WEEK)
    }

    @Test
    fun `GIVEN the rail WHEN the manager is created THEN the Featured batch sizes are requested`() = runTest {
        // Arrange
        val typeSlot = slot<GetMarketsTokenListFlowUseCase.BatchFlowType>()
        captureActions(typeSlot = typeSlot)

        // Act
        createManager(testScope = this)
        advanceUntilIdle()

        // Assert
        assertThat(typeSlot.captured).isEqualTo(GetMarketsTokenListFlowUseCase.BatchFlowType.Featured)
    }

    @Test
    fun `GIVEN loaded assets WHEN the interval changes THEN the rail re-maps without a new request`() = runTest {
        // Arrange — one token whose change percent has a different sign per interval
        val token = createToken(h24ChangePercent = PERCENT_UP, weekChangePercent = PERCENT_DOWN, monthChangePercent = null)
        val actions = captureActions(state = MutableStateFlow(stateOf(token)))
        val interval = MutableStateFlow(RwaInterval.H24)
        val manager = createManager(testScope = this, interval = interval)
        advanceUntilIdle()
        assertThat(manager.directions()).containsExactly(TangemPriceChange.Direction.Up)

        // Act
        interval.value = RwaInterval.D7
        advanceUntilIdle()

        // Assert
        assertThat(manager.directions()).containsExactly(TangemPriceChange.Direction.Down)
        assertThat(reloadConfigs(actions)).isEmpty()
    }

    @Test
    fun `GIVEN an interval the token has no percent for WHEN it is selected THEN the direction is neutral`() = runTest {
        // Arrange
        val token = createToken(h24ChangePercent = PERCENT_UP, weekChangePercent = PERCENT_DOWN, monthChangePercent = null)
        captureActions(state = MutableStateFlow(stateOf(token)))
        val interval = MutableStateFlow(RwaInterval.H24)
        val manager = createManager(testScope = this, interval = interval)
        advanceUntilIdle()

        // Act
        interval.value = RwaInterval.M1
        advanceUntilIdle()

        // Assert
        assertThat(manager.directions()).containsExactly(TangemPriceChange.Direction.Neutral)
    }

    @Test
    fun `GIVEN a curated bucket larger than five WHEN it loads THEN every asset is shown in order`() = runTest {
        // Arrange
        val tokens = (1..8).map { index -> createToken(id = "token-$index") }
        captureActions(state = MutableStateFlow(stateOf(*tokens.toTypedArray())))

        // Act
        val manager = createManager(testScope = this)
        advanceUntilIdle()

        // Assert
        val content = manager.state.value as RwaFeaturedUM.Content
        assertThat(content.assets.map { it.id }).containsExactlyElementsIn(tokens.map { it.id.value }).inOrder()
    }

    @Test
    fun `GIVEN the initial load failed WHEN the state is mapped THEN the rail is empty`() = runTest {
        // Arrange
        val state = MutableStateFlow(
            BatchListState<Int, List<TokenMarket>>(data = emptyList(), status = PaginationStatus.None),
        )
        captureActions(state = state)
        val manager = createManager(testScope = this)

        // Act
        state.value = BatchListState(
            data = emptyList(),
            status = PaginationStatus.InitialLoadingError(throwable = IllegalStateException("boom")),
        )
        advanceUntilIdle()

        // Assert
        assertThat(manager.state.value).isEqualTo(RwaFeaturedUM.Empty)
    }

    @Test
    fun `GIVEN a curated token absent from the list WHEN it is looked up by id THEN the rail resolves it`() = runTest {
        // Arrange — the tap handler falls back here, so the rail has to answer for its own tokens
        val token = createToken(id = "curated-only")
        captureActions(state = MutableStateFlow(stateOf(token)))
        val manager = createManager(testScope = this)
        advanceUntilIdle()

        // Act
        val resolved = manager.getTokenById(token.id)

        // Assert
        assertThat(resolved).isEqualTo(token)
        assertThat(manager.getTokenById(CryptoCurrency.RawID("never-loaded"))).isNull()
    }

    private fun RwaFeaturedAssetsManager.directions(): List<TangemPriceChange.Direction> {
        return (state.value as RwaFeaturedUM.Content).assets.map { it.priceChange.direction }
    }

    private fun stateOf(vararg tokens: TokenMarket): BatchListState<Int, List<TokenMarket>> {
        return BatchListState(
            data = listOf(Batch(key = 0, data = tokens.toList())),
            status = PaginationStatus.EndOfPagination,
        )
    }

    private fun createToken(
        id: String = "apple",
        name: String = "Apple",
        symbol: String = "AAPL",
        currentPrice: BigDecimal = BigDecimal("0.5"),
        h24ChangePercent: BigDecimal? = PERCENT_UP,
        weekChangePercent: BigDecimal? = PERCENT_DOWN,
        monthChangePercent: BigDecimal? = null,
        isRestrictedForCountry: Boolean = false,
    ) = TokenMarket(
        id = CryptoCurrency.RawID(id),
        name = name,
        symbol = symbol,
        marketRating = 1,
        marketCap = BigDecimal("0.9"),
        isUnderMarketCapLimit = false,
        tokenQuotesShort = TokenQuotesShort(
            currentPrice = currentPrice,
            h24ChangePercent = h24ChangePercent,
            weekChangePercent = weekChangePercent,
            monthChangePercent = monthChangePercent,
        ),
        tokenCharts = TokenMarket.Charts(h24 = null, week = null, month = null),
        yieldRate = null,
        updateTimestamp = null,
        networks = null,
        isRestrictedForCountry = isRestrictedForCountry,
        imageHost = "https://host/",
    )

    private fun TestScope.captureActions(
        state: MutableStateFlow<BatchListState<Int, List<TokenMarket>>> = MutableStateFlow(
            BatchListState(data = emptyList(), status = PaginationStatus.None),
        ),
        typeSlot: CapturingSlot<GetMarketsTokenListFlowUseCase.BatchFlowType>? = null,
    ): MutableList<BatchAction<Int, TokenMarketListConfig, TokenMarketUpdateRequest>> {
        val actions = mutableListOf<BatchAction<Int, TokenMarketListConfig, TokenMarketUpdateRequest>>()
        // PER_CLASS shares the mock, and a stale stub would hand the manager a previous test's fake
        clearMocks(getMarketsTokenListFlowUseCase)
        every {
            getMarketsTokenListFlowUseCase(any(), if (typeSlot != null) capture(typeSlot) else any())
        } answers {
            val context = firstArg<TokenListBatchingContext>()
            // undispatched: the actions flow is a rendezvous SharedFlow, which drops emissions made
            // while it has no subscriber — the collector has to be registered before the manager emits
            context.coroutineScope.launch(start = CoroutineStart.UNDISPATCHED) {
                context.actionsFlow.collect(actions::add)
            }
            FakeBatchFlow(state)
        }
        return actions
    }

    private fun reloadConfigs(
        actions: List<BatchAction<Int, TokenMarketListConfig, TokenMarketUpdateRequest>>,
    ): List<TokenMarketListConfig> = actions
        .filterIsInstance<BatchAction.Reload<TokenMarketListConfig>>()
        .map { it.requestParams }

    private fun createManager(
        testScope: TestScope,
        interval: MutableStateFlow<RwaInterval> = MutableStateFlow(RwaInterval.H24),
    ): RwaFeaturedAssetsManager {
        val testDispatcher = StandardTestDispatcher(testScope.testScheduler)
        // not backgroundScope: its jobs are not advanced by advanceUntilIdle here, and the manager's
        // collectors never complete, so it cannot be the test scope either
        val scope = CoroutineScope(testDispatcher + Job())
        scopes.add(scope)
        return RwaFeaturedAssetsManager(
            getMarketsTokenListFlowUseCase = getMarketsTokenListFlowUseCase,
            interval = interval,
            appCurrency = MutableStateFlow(AppCurrency.Default),
            onItemClick = {},
            modelScope = scope,
        )
    }

    private class FakeBatchFlow(
        override val state: StateFlow<BatchListState<Int, List<TokenMarket>>>,
    ) : TokenListBatchFlow {
        override val updateResults:
            SharedFlow<Pair<TokenMarketUpdateRequest, BatchUpdateResult<Int, List<TokenMarket>>>> =
            MutableSharedFlow()
    }

    private companion object {
        const val FEATURED_CATEGORY_ID = "featured-category-id"
        val PERCENT_UP: BigDecimal = BigDecimal("0.05")
        val PERCENT_DOWN: BigDecimal = BigDecimal("-0.02")
    }
}