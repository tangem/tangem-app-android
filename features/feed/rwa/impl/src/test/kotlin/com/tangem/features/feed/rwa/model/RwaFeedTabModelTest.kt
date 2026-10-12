package com.tangem.features.feed.rwa.model

import arrow.core.Either
import com.google.common.truth.Truth.assertThat
import com.tangem.core.analytics.api.AnalyticsEventHandler
import com.tangem.core.analytics.models.AnalyticsEvent
import com.tangem.core.decompose.navigation.Router
import com.tangem.domain.appcurrency.GetSelectedAppCurrencyUseCase
import com.tangem.domain.appcurrency.model.AppCurrency
import com.tangem.domain.markets.CoinCategory
import com.tangem.domain.markets.GetCoinCategoriesUseCase
import com.tangem.domain.markets.GetMarketsTokenListFlowUseCase
import com.tangem.domain.markets.TokenListBatchFlow
import com.tangem.domain.markets.TokenListBatchingContext
import com.tangem.domain.markets.TokenMarket
import com.tangem.domain.markets.TokenMarketListConfig
import com.tangem.domain.markets.TokenMarketUpdateRequest
import com.tangem.core.ui.ds2.tabnavigation.TangemTabItemUM
import com.tangem.features.feed.analytics.FeedSectionAnalyticsEvent
import com.tangem.features.feed.analytics.FeedSortPeriod
import com.tangem.features.feed.analytics.FeedSortType
import com.tangem.features.feed.nav.FeedTabId
import com.tangem.features.feed.rwa.categories.CategorySelection
import com.tangem.features.feed.rwa.categories.ReservedCategoryCodes
import com.tangem.features.feed.rwa.model.list.RwaInterval
import com.tangem.features.feed.rwa.model.list.RwaSort
import com.tangem.features.feed.rwa.ui.state.RwaFeaturedUM
import com.tangem.features.feed.rwa.ui.state.RwaListUM
import com.tangem.features.feed.v2.FeedEntryController
import com.tangem.features.feed.v2.FeedEntryFilter
import com.tangem.features.feed.v2.FeedEntryRequest
import com.tangem.pagination.BatchAction
import com.tangem.pagination.BatchListState
import com.tangem.pagination.BatchUpdateResult
import com.tangem.pagination.PaginationStatus
import com.tangem.test.core.createTestingCoroutineDispatcherProvider
import io.mockk.Runs
import io.mockk.clearMocks
import io.mockk.coEvery
import io.mockk.coVerify
import io.mockk.every
import io.mockk.just
import io.mockk.mockk
import kotlinx.coroutines.CompletableDeferred
import kotlinx.coroutines.CoroutineStart
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharedFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.flowOf
import kotlinx.coroutines.launch
import kotlinx.coroutines.test.TestScope
import kotlinx.coroutines.test.advanceUntilIdle
import kotlinx.coroutines.test.runTest
import org.junit.jupiter.api.AfterEach
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.TestInstance

/**
 * The tab cannot request its list until the categories fetch resolves the `real-world-assets` umbrella id —
 * an unscoped request would answer with the whole coin universe. That ordering is the contract these tests
 * pin, along with what each half of it does when the other fails.
 *
 * The tab builds two batch flows from one use case, so the fake keys them on the requested
 * [GetMarketsTokenListFlowUseCase.BatchFlowType]: the rail is `Featured`, the list is everything else.
 */
@OptIn(ExperimentalCoroutinesApi::class)
@TestInstance(TestInstance.Lifecycle.PER_CLASS)
internal class RwaFeedTabModelTest {

    private val getMarketsTokenListFlowUseCase: GetMarketsTokenListFlowUseCase = mockk()
    private val getSelectedAppCurrencyUseCase: GetSelectedAppCurrencyUseCase = mockk()

    // never relaxed: a relaxed Either invokes neither fold branch, so the umbrella would stay unresolved
    // and every "list was not requested" assertion would pass for the wrong reason
    private val getCoinCategoriesUseCase: GetCoinCategoriesUseCase = mockk()
    private val userCountryResolver: UserCountryResolver = mockk()
    private val router: Router = mockk(relaxed = true)
    private val analyticsEventHandler: AnalyticsEventHandler = mockk(relaxed = true)
    private val sentEvents = mutableListOf<AnalyticsEvent>()

    private val listActions = mutableListOf<BatchAction<Int, TokenMarketListConfig, TokenMarketUpdateRequest>>()
    private val featuredActions = mutableListOf<BatchAction<Int, TokenMarketListConfig, TokenMarketUpdateRequest>>()

    private val pendingFilter = MutableStateFlow<FeedEntryFilter?>(null)
    private val feedEntryController = FakeFeedEntryController(pendingFilter)

    private var model: RwaFeedTabModel? = null

    @BeforeEach
    fun setUp() {
        clearMocks(
            getMarketsTokenListFlowUseCase,
            getSelectedAppCurrencyUseCase,
            getCoinCategoriesUseCase,
            userCountryResolver,
            router,
            analyticsEventHandler,
        )
        listActions.clear()
        featuredActions.clear()
        sentEvents.clear()
        pendingFilter.value = null
        every { analyticsEventHandler.send(capture(sentEvents)) } just Runs

        coEvery { userCountryResolver.resolve() } returns "DE"

        every { getSelectedAppCurrencyUseCase() } returns flowOf(Either.Right(AppCurrency.Default))
        every { getMarketsTokenListFlowUseCase(any(), any()) } answers {
            val context = firstArg<TokenListBatchingContext>()
            val isFeatured = secondArg<GetMarketsTokenListFlowUseCase.BatchFlowType>() ==
                GetMarketsTokenListFlowUseCase.BatchFlowType.Featured
            val sink = if (isFeatured) featuredActions else listActions

            // undispatched: the actions flow is a rendezvous SharedFlow, which drops emissions made
            // while it has no subscriber
            context.coroutineScope.launch(start = CoroutineStart.UNDISPATCHED) {
                context.actionsFlow.collect(sink::add)
            }
            FakeBatchFlow()
        }
    }

    @AfterEach
    fun destroyModel() {
        model?.onDestroy()
        model = null
    }

    @Test
    fun `GIVEN categories not yet loaded WHEN the tab opens THEN the list is not requested`() = runTest {
        // Arrange — the fetch never completes, so the umbrella id is never resolved
        coEvery { getCoinCategoriesUseCase(country = "DE") } coAnswers { CompletableDeferred<Nothing>().await() }

        // Act
        createModel(testScope = this)
        advanceUntilIdle()

        // Assert
        assertThat(reloadConfigs(listActions)).isEmpty()
        assertThat(reloadConfigs(featuredActions)).isEmpty()
    }

    @Test
    fun `GIVEN categories load WHEN the umbrella resolves THEN the list is requested scoped to it`() = runTest {
        // Arrange
        coEvery { getCoinCategoriesUseCase(country = "DE") } returns Either.Right(listOf(umbrellaCategory()))

        // Act
        createModel(testScope = this)
        advanceUntilIdle()

        // Assert
        assertThat(reloadConfigs(listActions).map { it.categoryId }).containsExactly(UMBRELLA_ID)
    }

    @Test
    fun `GIVEN featured-assets is present WHEN categories load THEN the rail is requested for it`() = runTest {
        // Arrange
        coEvery { getCoinCategoriesUseCase(country = "DE") } returns Either.Right(
            listOf(umbrellaCategory(), featuredCategory()),
        )

        // Act
        createModel(testScope = this)
        advanceUntilIdle()

        // Assert — the curated order is the whole point of the rail, so ByCategory has to survive
        val railConfig = reloadConfigs(featuredActions).single()
        assertThat(railConfig.categoryId).isEqualTo(FEATURED_ID)
        assertThat(railConfig.order).isEqualTo(TokenMarketListConfig.Order.ByCategory)
    }

    @Test
    fun `GIVEN a resolved country WHEN the rail is requested THEN it travels with the request`() = runTest {
        // Arrange — without it the curated tokens come back unrestricted, and the rail opens a
        // blocked asset past the region gate on the token screen
        coEvery { userCountryResolver.resolve() } returns "US"
        coEvery { getCoinCategoriesUseCase(country = "US") } returns Either.Right(
            listOf(umbrellaCategory(), featuredCategory(), commoditiesCategory()),
        )

        // Act
        createModel(testScope = this)
        advanceUntilIdle()

        // Assert
        assertThat(reloadConfigs(featuredActions).map { it.country }).containsExactly("US")
    }

    @Test
    fun `GIVEN featured-assets is missing WHEN categories load THEN the rail is empty and the list still loads`() =
        runTest {
            // Arrange
            coEvery { getCoinCategoriesUseCase(country = "DE") } returns Either.Right(listOf(umbrellaCategory()))

            // Act
            val underTest = createModel(testScope = this)
            advanceUntilIdle()

            // Assert — a missing reserved code degrades the rail rather than taking the list with it
            assertThat(underTest.uiState.value.featured).isEqualTo(RwaFeaturedUM.Empty)
            assertThat(reloadConfigs(featuredActions)).isEmpty()
            assertThat(reloadConfigs(listActions).map { it.categoryId }).containsExactly(UMBRELLA_ID)
        }

    @Test
    fun `GIVEN the categories fetch fails WHEN the tab opens THEN the list errors without being requested`() =
        runTest {
            // Arrange
            coEvery { getCoinCategoriesUseCase(country = "DE") } returns Either.Left(IllegalStateException("boom"))

            // Act
            val underTest = createModel(testScope = this)
            advanceUntilIdle()

            // Assert
            assertThat(underTest.uiState.value.list).isInstanceOf(RwaListUM.Error::class.java)
            assertThat(reloadConfigs(listActions)).isEmpty()
        }

    @Test
    fun `GIVEN the umbrella is missing WHEN categories load THEN the list errors without being requested`() = runTest {
        // Arrange — the response is fine, but carries no real-world-assets code
        coEvery { getCoinCategoriesUseCase(country = "DE") } returns Either.Right(listOf(featuredCategory()))

        // Act
        val underTest = createModel(testScope = this)
        advanceUntilIdle()

        // Assert
        assertThat(underTest.uiState.value.list).isInstanceOf(RwaListUM.Error::class.java)
        assertThat(reloadConfigs(listActions)).isEmpty()
    }

    @Test
    fun `GIVEN a categories failure WHEN retry is tapped THEN categories are refetched before the list`() = runTest {
        // Arrange
        coEvery { getCoinCategoriesUseCase(country = "DE") } returnsMany listOf(
            Either.Left(IllegalStateException("boom")),
            Either.Right(listOf(umbrellaCategory())),
        )
        val underTest = createModel(testScope = this)
        advanceUntilIdle()
        val error = underTest.uiState.value.list as RwaListUM.Error

        // Act
        error.onRetry()
        advanceUntilIdle()

        // Assert — the retry re-resolves the umbrella first; a bare list reload would go out unscoped
        coVerify(exactly = 2) { getCoinCategoriesUseCase(country = "DE") }
        assertThat(reloadConfigs(listActions).map { it.categoryId }).containsExactly(UMBRELLA_ID)
    }

    @Test
    fun `GIVEN a restricted country WHEN categories load THEN the tab opens on commodities`() = runTest {
        // Arrange
        coEvery { userCountryResolver.resolve() } returns "US"
        coEvery { getCoinCategoriesUseCase(country = "US") } returns Either.Right(
            listOf(umbrellaCategory(), commoditiesCategory()),
        )

        // Act
        val underTest = createModel(testScope = this)
        advanceUntilIdle()

        // Assert — the resolved country reaches the fetch, and the default it yields scopes the list
        coVerify(exactly = 1) { getCoinCategoriesUseCase(country = "US") }
        assertThat(underTest.uiState.value.appliedSelection.category?.code)
            .isEqualTo(ReservedCategoryCodes.COMMODITIES)
        assertThat(reloadConfigs(listActions).map { it.categoryId }).containsExactly(COMMODITIES_ID)
    }

    @Test
    fun `GIVEN no country WHEN categories load THEN no default is applied and the list keeps the umbrella`() =
        runTest {
            // Arrange — a response fetched without a country comes back with every category unrestricted,
            // so it carries nothing to derive a default from
            coEvery { userCountryResolver.resolve() } returns null
            coEvery { getCoinCategoriesUseCase(country = null) } returns Either.Right(
                listOf(umbrellaCategory(), commoditiesCategory()),
            )

            // Act
            val underTest = createModel(testScope = this)
            advanceUntilIdle()

            // Assert
            assertThat(underTest.uiState.value.appliedSelection.category).isNull()
            assertThat(reloadConfigs(listActions).map { it.categoryId }).containsExactly(UMBRELLA_ID)
        }

    @Test
    fun `GIVEN a pick made before the first load succeeds WHEN it succeeds THEN the default stands aside`() =
        runTest {
            // Arrange — the first fetch fails, so the pick lands while no umbrella is resolved yet
            coEvery { userCountryResolver.resolve() } returns "US"
            coEvery { getCoinCategoriesUseCase(country = "US") } returnsMany listOf(
                Either.Left(IllegalStateException("boom")),
                Either.Right(listOf(umbrellaCategory(), commoditiesCategory())),
            )
            val underTest = createModel(testScope = this)
            advanceUntilIdle()

            // Act — selecting re-runs the categories fetch, which now succeeds and offers its default
            underTest.onCategorySelect(
                CategorySelection(
                    category = CategorySelection.Entry(id = PICKED_ID, displayName = "Stocks", code = "stocks"),
                ),
            )
            advanceUntilIdle()

            // Assert
            assertThat(underTest.uiState.value.appliedSelection.category?.id).isEqualTo(PICKED_ID)
            assertThat(reloadConfigs(listActions).map { it.categoryId }).containsExactly(PICKED_ID)
        }

    @Test
    fun `GIVEN the tab is built WHEN the user touches nothing THEN nothing is reported`() = runTest {
        // Arrange — every tab model is built when the feed opens, including ones never visited
        coEvery { getCoinCategoriesUseCase(country = "DE") } returns Either.Right(listOf(umbrellaCategory()))

        // Act
        createModel(testScope = this)
        advanceUntilIdle()

        // Assert
        assertThat(sectionEvents()).isEmpty()
    }

    @Test
    fun `GIVEN the tab open WHEN the sort changes THEN sort by is reported for the assets section`() = runTest {
        // Arrange
        coEvery { getCoinCategoriesUseCase(country = "DE") } returns Either.Right(listOf(umbrellaCategory()))
        val underTest = createModel(testScope = this)
        advanceUntilIdle()

        // Act
        underTest.sortTab(RwaSort.TopGainers).onClick()
        advanceUntilIdle()

        // Assert
        assertThat(sectionEvents()).containsExactly(
            FeedSectionAnalyticsEvent.SortBy(
                section = "Assets",
                type = FeedSortType.Gainers,
                period = FeedSortPeriod.H24,
            ),
        )
    }

    @Test
    fun `GIVEN the tab open WHEN the interval changes THEN sort by is reported with the new period`() = runTest {
        // Arrange
        coEvery { getCoinCategoriesUseCase(country = "DE") } returns Either.Right(listOf(umbrellaCategory()))
        val underTest = createModel(testScope = this)
        advanceUntilIdle()

        // Act
        underTest.uiState.value.interval.onOptionClick(RwaInterval.D7)
        advanceUntilIdle()

        // Assert
        assertThat(sectionEvents()).containsExactly(
            FeedSectionAnalyticsEvent.SortBy(
                section = "Assets",
                type = FeedSortType.Rating,
                period = FeedSortPeriod.D7,
            ),
        )
    }

    @Test
    fun `GIVEN the tab open WHEN a category is picked THEN select category carries its stable code`() = runTest {
        // Arrange
        coEvery { getCoinCategoriesUseCase(country = "DE") } returns Either.Right(listOf(umbrellaCategory()))
        val underTest = createModel(testScope = this)
        advanceUntilIdle()

        // Act
        underTest.onCategorySelect(
            CategorySelection(
                category = CategorySelection.Entry(id = "1", displayName = "Treasuries", code = "treasuries"),
            ),
        )
        advanceUntilIdle()

        // Assert
        assertThat(sectionEvents()).containsExactly(
            FeedSectionAnalyticsEvent.SelectCategory(section = "Assets", categoryId = "treasuries"),
        )
    }

    @Test
    fun `GIVEN the tab open WHEN the filter is cleared THEN select category reports All`() = runTest {
        // Arrange
        coEvery { getCoinCategoriesUseCase(country = "DE") } returns Either.Right(listOf(umbrellaCategory()))
        val underTest = createModel(testScope = this)
        advanceUntilIdle()

        // Act
        underTest.onCategorySelect(CategorySelection())
        advanceUntilIdle()

        // Assert
        assertThat(sectionEvents()).containsExactly(
            FeedSectionAnalyticsEvent.SelectCategory(section = "Assets", categoryId = "All"),
        )
    }

    @Test
    fun `GIVEN a picked sector WHEN reported THEN only the category level travels`() = runTest {
        // Arrange
        coEvery { getCoinCategoriesUseCase(country = "DE") } returns Either.Right(listOf(umbrellaCategory()))
        val underTest = createModel(testScope = this)
        advanceUntilIdle()

        // Act
        underTest.onCategorySelect(
            CategorySelection(
                category = CategorySelection.Entry(id = "1", displayName = "Treasuries", code = "treasuries"),
                sector = CategorySelection.Entry(id = "2", displayName = "US Bills", code = "us-bills"),
            ),
        )
        advanceUntilIdle()

        // Assert
        assertThat(sectionEvents()).containsExactly(
            FeedSectionAnalyticsEvent.SelectCategory(section = "Assets", categoryId = "treasuries"),
        )
    }

    @Test
    fun `GIVEN reported events WHEN inspected THEN they carry the agreed params`() = runTest {
        // Arrange
        coEvery { getCoinCategoriesUseCase(country = "DE") } returns Either.Right(listOf(umbrellaCategory()))
        val underTest = createModel(testScope = this)
        advanceUntilIdle()

        // Act
        underTest.sortTab(RwaSort.TopLosers).onClick()
        underTest.onCategorySelect(
            CategorySelection(
                category = CategorySelection.Entry(id = "1", displayName = "Treasuries", code = "treasuries"),
            ),
        )
        advanceUntilIdle()

        // Assert
        val sortBy = sectionEvents().first { it is FeedSectionAnalyticsEvent.SortBy }
        assertThat(sortBy.id).isEqualTo("[Markets] Sort By")
        assertThat(sortBy.params).containsExactlyEntriesIn(
            mapOf("Section" to "Assets", "Type" to "Losers", "Period" to "24h"),
        )

        val selectCategory = sectionEvents().first { it is FeedSectionAnalyticsEvent.SelectCategory }
        assertThat(selectCategory.id).isEqualTo("[Markets] Select Category")
        assertThat(selectCategory.params).containsExactlyEntriesIn(
            mapOf("Section" to "Assets", "Category" to "treasuries"),
        )
    }

    @Test
    fun `GIVEN a category deeplink WHEN the tab opens THEN the list is requested once, already filtered`() = runTest {
        // Arrange
        coEvery { getCoinCategoriesUseCase(country = "DE") } returns Either.Right(
            listOf(umbrellaCategory(), category(id = STOCKS_ID, code = STOCKS_CODE)),
        )
        pendingFilter.value = FeedEntryFilter(tabId = FeedTabId.RealAssets, categoryId = STOCKS_CODE)

        // Act
        createModel(testScope = this)
        advanceUntilIdle()

        // Assert — requesting the umbrella first would show the wrong list and then swap it out
        assertThat(reloadConfigs(listActions).map { it.categoryId }).containsExactly(STOCKS_ID)
        assertThat(pendingFilter.value).isNull()
        // the host reports the open; the tab must not also report a pick the user never made
        assertThat(sectionEvents()).isEmpty()
    }

    @Test
    fun `GIVEN loaded categories WHEN a deeplink lands THEN it is applied without refetching them`() = runTest {
        // Arrange
        coEvery { getCoinCategoriesUseCase(country = "DE") } returns Either.Right(
            listOf(umbrellaCategory(), category(id = STOCKS_ID, code = STOCKS_CODE)),
        )
        createModel(testScope = this)
        advanceUntilIdle()

        // Act — the id is the other identifier a deeplink may carry
        pendingFilter.value = FeedEntryFilter(tabId = FeedTabId.RealAssets, categoryId = STOCKS_ID)
        advanceUntilIdle()

        // Assert — the umbrella request is the one made before the deeplink arrived
        assertThat(reloadConfigs(listActions).map { it.categoryId })
            .containsExactly(UMBRELLA_ID, STOCKS_ID)
            .inOrder()
        coVerify(exactly = 1) { getCoinCategoriesUseCase(country = "DE") }
        assertThat(pendingFilter.value).isNull()
        assertThat(sectionEvents()).isEmpty()
    }

    @Test
    fun `GIVEN a deeplink applied the filter WHEN the user then picks one THEN only that pick is reported`() =
        runTest {
            // Arrange
            coEvery { getCoinCategoriesUseCase(country = "DE") } returns Either.Right(
                listOf(umbrellaCategory(), category(id = STOCKS_ID, code = STOCKS_CODE)),
            )
            pendingFilter.value = FeedEntryFilter(tabId = FeedTabId.RealAssets, categoryId = STOCKS_CODE)
            val underTest = createModel(testScope = this)
            advanceUntilIdle()

            // Act
            underTest.onCategorySelect(
                CategorySelection(
                    category = CategorySelection.Entry(id = "2", displayName = "Bonds", code = "bonds"),
                ),
            )
            advanceUntilIdle()

            // Assert
            assertThat(sectionEvents()).containsExactly(
                FeedSectionAnalyticsEvent.SelectCategory(section = "Assets", categoryId = "bonds"),
            )
        }

    @Test
    fun `GIVEN a category nothing matches WHEN the tab opens THEN it loads scoped to the umbrella`() = runTest {
        // Arrange
        coEvery { getCoinCategoriesUseCase(country = "DE") } returns Either.Right(listOf(umbrellaCategory()))
        pendingFilter.value = FeedEntryFilter(tabId = FeedTabId.RealAssets, categoryId = "made-up")

        // Act
        createModel(testScope = this)
        advanceUntilIdle()

        // Assert
        assertThat(reloadConfigs(listActions).map { it.categoryId }).containsExactly(UMBRELLA_ID)
        assertThat(pendingFilter.value).isNull()
    }

    @Test
    fun `GIVEN a deeplink for another tab WHEN the tab opens THEN it is left parked and nothing is filtered`() =
        runTest {
            // Arrange
            coEvery { getCoinCategoriesUseCase(country = "DE") } returns Either.Right(listOf(umbrellaCategory()))
            pendingFilter.value = FeedEntryFilter(tabId = FeedTabId.Crypto, categoryId = "layer-1")

            // Act
            createModel(testScope = this)
            advanceUntilIdle()

            // Assert — only the addressed tab may clear the slot
            assertThat(reloadConfigs(listActions).map { it.categoryId }).containsExactly(UMBRELLA_ID)
            assertThat(pendingFilter.value).isNotNull()
        }

    private fun sectionEvents() = sentEvents.filterIsInstance<FeedSectionAnalyticsEvent>()

    /** The tab carries its own `onClick`, so the enum's id is the only bridge from a sort to its tab. */
    private fun RwaFeedTabModel.sortTab(sort: RwaSort): TangemTabItemUM.Content = uiState
        .value
        .sortTabs
        .filterIsInstance<TangemTabItemUM.Content>()
        .first { it.id == sort.id }

    private fun reloadConfigs(
        actions: List<BatchAction<Int, TokenMarketListConfig, TokenMarketUpdateRequest>>,
    ): List<TokenMarketListConfig> = actions
        .filterIsInstance<BatchAction.Reload<TokenMarketListConfig>>()
        .map { it.requestParams }

    private fun createModel(testScope: TestScope): RwaFeedTabModel = RwaFeedTabModel(
        dispatchers = testScope.createTestingCoroutineDispatcherProvider(),
        getMarketsTokenListFlowUseCase = getMarketsTokenListFlowUseCase,
        getSelectedAppCurrencyUseCase = getSelectedAppCurrencyUseCase,
        getCoinCategoriesUseCase = getCoinCategoriesUseCase,
        userCountryResolver = userCountryResolver,
        feedEntryController = feedEntryController,
        analyticsEventHandler = analyticsEventHandler,
        router = router,
    ).also { model = it }

    private fun umbrellaCategory() = category(id = UMBRELLA_ID, code = ReservedCategoryCodes.REAL_WORLD_ASSETS)

    private fun commoditiesCategory() = category(id = COMMODITIES_ID, code = ReservedCategoryCodes.COMMODITIES)

    private fun featuredCategory() = category(id = FEATURED_ID, code = ReservedCategoryCodes.FEATURED_ASSETS)

    private fun category(id: String, code: String) = CoinCategory(
        id = id,
        code = code,
        displayName = code,
        displayOrder = 0,
        tokensCount = 42,
        isRestricted = false,
        sectors = emptyList(),
        isVisible = true,
    )

    private class FakeFeedEntryController(
        override val pendingFilter: MutableStateFlow<FeedEntryFilter?>,
    ) : FeedEntryController {

        override val pendingEntry = MutableStateFlow<FeedEntryRequest?>(null)

        override fun request(request: FeedEntryRequest) = Unit

        override fun consumeEntry() {
            pendingEntry.value = null
        }

        override fun consumeFilter() {
            pendingFilter.value = null
        }
    }

    private class FakeBatchFlow(
        override val state: StateFlow<BatchListState<Int, List<TokenMarket>>> = MutableStateFlow(
            BatchListState(data = emptyList(), status = PaginationStatus.None),
        ),
    ) : TokenListBatchFlow {
        override val updateResults:
            SharedFlow<Pair<TokenMarketUpdateRequest, BatchUpdateResult<Int, List<TokenMarket>>>> =
            MutableSharedFlow()
    }

    private companion object {
        const val UMBRELLA_ID = "rwa-umbrella"
        const val COMMODITIES_ID = "rwa-commodities"
        const val PICKED_ID = "rwa-picked"
        const val FEATURED_ID = "rwa-featured"
        const val STOCKS_ID = "rwa-stocks"
        const val STOCKS_CODE = "stocks"
    }
}