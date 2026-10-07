package com.tangem.features.feed.crypto.model

import arrow.core.left
import arrow.core.right
import com.arkivanov.decompose.router.slot.SlotNavigation
import com.google.common.truth.Truth.assertThat
import com.tangem.core.analytics.api.AnalyticsEventHandler
import com.tangem.core.analytics.models.AnalyticsEvent
import com.tangem.core.decompose.navigation.Router
import com.tangem.core.navigation.url.UrlOpener
import com.tangem.core.ui.ds2.filter.TangemFilterItemUM
import com.tangem.features.feed.analytics.FeedSectionAnalyticsEvent
import com.tangem.features.feed.analytics.FeedSortPeriod
import com.tangem.features.feed.analytics.FeedSortType
import com.tangem.core.ui.extensions.TextReference
import com.tangem.domain.appcurrency.GetSelectedAppCurrencyUseCase
import com.tangem.domain.appcurrency.model.AppCurrency
import com.tangem.domain.markets.GetMarketIndicatorsUseCase
import com.tangem.domain.markets.GetMarketsTokenListFlowUseCase
import com.tangem.domain.markets.MarketIndicators
import com.tangem.domain.markets.TokenListBatchFlow
import com.tangem.domain.markets.TokenListBatchingContext
import com.tangem.domain.markets.TokenMarket
import com.tangem.domain.markets.TokenMarketListConfig
import com.tangem.domain.markets.TokenMarketUpdateRequest
import com.tangem.domain.models.earn.PreselectedEarnType
import com.tangem.features.feed.crypto.categories.MarketCategoryFilterController
import com.tangem.features.feed.crypto.categories.MarketCategoryResolver
import com.tangem.features.feed.crypto.categories.MarketCategorySelection
import com.tangem.features.feed.crypto.categories.MarketCategorySelectorRoute
import com.tangem.features.feed.crypto.impl.R
import com.tangem.core.ui.ds2.tabnavigation.TangemTabItemUM
import com.tangem.features.feed.crypto.model.entity.FeedCryptoTabBottomSheetConfig
import com.tangem.features.feed.crypto.model.list.MarketPulseCategory
import com.tangem.features.feed.nav.FeedTabId
import com.tangem.features.feed.v2.FeedEntryController
import com.tangem.features.feed.v2.FeedEntryFilter
import com.tangem.features.feed.v2.FeedEntryRequest
import com.tangem.features.feed.crypto.model.list.MarketPulseCategory.ExperiencedBuyers
import com.tangem.features.feed.crypto.model.list.MarketPulseCategory.MarketCap
import com.tangem.features.feed.crypto.model.list.MarketPulseCategory.TopGainers
import com.tangem.features.feed.crypto.model.list.MarketPulseCategory.TopLosers
import com.tangem.features.feed.crypto.model.list.MarketPulseInterval
import com.tangem.features.feed.crypto.ui.state.MarketPulseIntervalUM
import com.tangem.features.feed.crypto.ui.state.TotalMarketCapUM
import com.tangem.pagination.BatchAction
import com.tangem.pagination.BatchListState
import com.tangem.pagination.BatchUpdateResult
import com.tangem.pagination.PaginationStatus
import com.tangem.utils.coroutines.TestingCoroutineDispatcherProvider
import io.mockk.clearMocks
import io.mockk.coEvery
import io.mockk.Runs
import io.mockk.coVerify
import io.mockk.coVerifyOrder
import io.mockk.every
import io.mockk.just
import io.mockk.mockk
import io.mockk.slot
import io.mockk.verify
import kotlinx.coroutines.CoroutineStart
import kotlinx.coroutines.delay
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharedFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.flowOf
import kotlinx.coroutines.launch
import kotlinx.coroutines.test.StandardTestDispatcher
import kotlinx.coroutines.test.TestScope
import kotlinx.coroutines.test.advanceTimeBy
import kotlinx.coroutines.test.advanceUntilIdle
import kotlinx.coroutines.test.runCurrent
import kotlinx.coroutines.test.runTest
import org.junit.jupiter.api.AfterEach
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Test
import java.math.BigDecimal

/**
 * The tab renders the category chip and narrows its list request, but does not own the selection —
 * that lives in [MarketCategoryFilterController], the seam it shares with the selector screen. So the
 * tests drive the controller and assert what the tab does about it.
 *
 * Never drive [CryptoFeedTabModel.containerBottomSheetState] to EXPANDED together with
 * `isVisibleOnScreen`: the quotes timer is a `while (true) { delay(…) }` that only parks because the
 * sheet stays collapsed, and releasing it turns `advanceUntilIdle` into an infinite loop.
 */
@OptIn(ExperimentalCoroutinesApi::class)
internal class CryptoFeedTabModelTest {

    private val getMarketsTokenListFlowUseCase: GetMarketsTokenListFlowUseCase = mockk()
    private val getSelectedAppCurrencyUseCase: GetSelectedAppCurrencyUseCase = mockk()
    private val getMarketIndicatorsUseCase: GetMarketIndicatorsUseCase = mockk()
    private val router: Router = mockk(relaxed = true)
    private val urlOpener: UrlOpener = mockk(relaxed = true)
    private val analyticsEventHandler: AnalyticsEventHandler = mockk(relaxed = true)
    private val sentEvents = mutableListOf<AnalyticsEvent>()

    private val filterController = MarketCategoryFilterController()
    private val categoryResolver: MarketCategoryResolver = mockk()
    private val pendingFilter = MutableStateFlow<FeedEntryFilter?>(null)
    private val feedEntryController = FakeFeedEntryController(pendingFilter)
    private val actions = mutableListOf<BatchAction<Int, TokenMarketListConfig, TokenMarketUpdateRequest>>()
    private var model: CryptoFeedTabModel? = null

    @BeforeEach
    fun resetMocks() {
        clearMocks(
            getMarketsTokenListFlowUseCase,
            getSelectedAppCurrencyUseCase,
            getMarketIndicatorsUseCase,
            router,
            analyticsEventHandler,
        )
        actions.clear()
        sentEvents.clear()
        every { analyticsEventHandler.send(capture(sentEvents)) } just Runs
        every { getSelectedAppCurrencyUseCase() } returns flowOf(AppCurrency.Default.right())
        coEvery { getMarketIndicatorsUseCase(any()) } returns noIndicators.right()
    }

    @AfterEach
    fun destroyModel() {
        model?.onDestroy()
        model = null
    }

    @Test
    fun `GIVEN no filter applied WHEN the tab opens THEN the chip invites the user to pick a category`() = runTest {
        // Act
        val model = createModel(testScope = this)
        advanceUntilIdle()

        // Assert
        val chip = model.chip() as TangemFilterItemUM.Inactive
        assertThat(chip.id).isEqualTo("crypto_categories")
        assertThat(chip.label).isEqualTo(TextReference.Res(R.string.market_categories))
    }

    @Test
    fun `GIVEN the selector applied a category WHEN the tab sees it THEN the chip shows its name`() = runTest {
        // Arrange
        val model = createModel(testScope = this)
        advanceUntilIdle()

        // Act
        filterController.onSelectionApplied(selectionOf(id = "layer-1", name = "Layer 1"))
        advanceUntilIdle()

        // Assert
        val chip = model.chip() as TangemFilterItemUM.Active
        assertThat(chip.id).isEqualTo("crypto_categories")
        assertThat(chip.value).isEqualTo(TextReference.Str("Layer 1"))
    }

    @Test
    fun `GIVEN categories applied in turn WHEN the chip follows them THEN its id never changes`() = runTest {
        // Arrange — TangemFilterGroup keys its row on the id, so a change rebuilds the chip
        // as a different item and its crossfade never runs
        val model = createModel(testScope = this)
        advanceUntilIdle()
        val whenInactive = model.chip()?.id

        // Act
        filterController.onSelectionApplied(selectionOf(id = "layer-1", name = "Layer 1"))
        advanceUntilIdle()
        val whenActive = model.chip()?.id

        filterController.onSelectionApplied(selectionOf(id = "meme", name = "Meme"))
        advanceUntilIdle()
        val whenSwitched = model.chip()?.id

        // Assert
        assertThat(whenInactive).isEqualTo("crypto_categories")
        assertThat(whenActive).isEqualTo(whenInactive)
        assertThat(whenSwitched).isEqualTo(whenInactive)
    }

    @Test
    fun `GIVEN the selector applied a category WHEN the tab sees it THEN the list is refetched for it`() = runTest {
        // Arrange
        val model = createModel(testScope = this)
        advanceUntilIdle()

        // Act
        filterController.onSelectionApplied(selectionOf(id = "layer-1", name = "Layer 1"))
        advanceUntilIdle()

        // Assert — the opening request carried no category, the reload carries it
        assertThat(actions.reloadCategoryIds()).containsExactly(null, "layer-1").inOrder()
        assertThat(model.chip()).isInstanceOf(TangemFilterItemUM.Active::class.java)
    }

    @Test
    fun `GIVEN a filter applied WHEN the chip is cleared THEN it goes back to inviting and the list is unfiltered`() =
        runTest {
            // Arrange
            val model = createModel(testScope = this)
            advanceUntilIdle()
            filterController.onSelectionApplied(selectionOf(id = "layer-1", name = "Layer 1"))
            advanceUntilIdle()

            // Act
            (model.chip() as TangemFilterItemUM.Active).onClearClick()
            advanceUntilIdle()

            // Assert — clearing takes the same route through the controller as applying
            assertThat(filterController.selection.value).isEqualTo(MarketCategorySelection())
            assertThat(model.chip()).isInstanceOf(TangemFilterItemUM.Inactive::class.java)
            assertThat(actions.reloadCategoryIds()).containsExactly(null, "layer-1", null).inOrder()
        }

    @Test
    fun `GIVEN the chip WHEN it is clicked THEN the selector screen opens`() = runTest {
        // Arrange
        val model = createModel(testScope = this)
        advanceUntilIdle()

        // Act
        (model.chip() as TangemFilterItemUM.Inactive).onClick()
        advanceUntilIdle()

        // Assert — the route carries nothing; the screen reads the applied filter from the controller
        val routeSlot = slot<MarketCategorySelectorRoute>()
        verify { router.push(capture(routeSlot)) }
        assertThat(routeSlot.captured).isEqualTo(MarketCategorySelectorRoute)
    }

    @Test
    fun `GIVEN a filter applied WHEN the tab is destroyed THEN a reopened feed starts unfiltered`() = runTest {
        // Arrange — the controller outlives the tab, so the tab has to drop the filter with itself
        val model = createModel(testScope = this)
        advanceUntilIdle()
        filterController.onSelectionApplied(selectionOf(id = "layer-1", name = "Layer 1"))
        advanceUntilIdle()

        // Act
        model.onDestroy()

        // Assert
        assertThat(filterController.selection.value).isEqualTo(MarketCategorySelection())
    }

    @Test
    fun `GIVEN the interval menu WHEN an option is picked THEN the chip follows it`() = runTest {
        // Arrange
        val model = createModel(testScope = this)
        advanceUntilIdle()

        // Act
        model.intervalMenu().onOptionClick(MarketPulseInterval.M1)
        advanceUntilIdle()

        // Assert
        assertThat(model.intervalMenu().selected).isEqualTo(MarketPulseInterval.M1)
    }

    @Test
    fun `GIVEN the applied interval WHEN it is picked again THEN nothing is refetched`() = runTest {
        // Arrange — the menu offers every option, including the one already applied
        val model = createModel(testScope = this)
        advanceUntilIdle()
        val actionsAfterOpening = actions.size

        // Act
        model.intervalMenu().onOptionClick(model.intervalMenu().selected)
        advanceUntilIdle()

        // Assert
        assertThat(actions).hasSize(actionsAfterOpening)
    }

    @Test
    fun `GIVEN the list sorted by market cap WHEN the interval changes THEN it is not refetched`() = runTest {
        // Arrange — rating order does not depend on the interval, so the rows are reused and only
        // their charts are refreshed
        val model = createModel(testScope = this)
        advanceUntilIdle()

        // Act
        model.intervalMenu().onOptionClick(MarketPulseInterval.D7)
        advanceUntilIdle()

        // Assert
        assertThat(actions.reloadCount()).isEqualTo(1)
    }

    @Test
    fun `GIVEN the list sorted by gainers WHEN the interval changes THEN it is refetched`() = runTest {
        // Arrange — every other order ranks by the interval, so the backend has to re-rank
        val model = createModel(testScope = this)
        advanceUntilIdle()
        model.sortTab(TopGainers).onClick()
        advanceUntilIdle()
        val reloadsBefore = actions.reloadCount()

        // Act
        model.intervalMenu().onOptionClick(MarketPulseInterval.D7)
        advanceUntilIdle()

        // Assert
        assertThat(actions.reloadCount()).isEqualTo(reloadsBefore + 1)
    }

    @Test
    fun `GIVEN no filter applied WHEN the tab opens THEN buyers is offered and trending is not`() = runTest {
        // Act
        val model = createModel(testScope = this)
        advanceUntilIdle()

        // Assert
        assertThat(model.sortTabIds()).containsExactly(
            MarketCap.id,
            TopGainers.id,
            TopLosers.id,
            ExperiencedBuyers.id,
        ).inOrder()
    }

    @Test
    fun `GIVEN a category applied WHEN the tabs render THEN buyers is not offered`() = runTest {
        // Arrange — the backend ranks buyers across the whole market only and returns nothing under a
        // CoinGecko category
        val model = createModel(testScope = this)
        advanceUntilIdle()

        // Act
        filterController.onSelectionApplied(selectionOf(id = "layer-1", name = "Layer 1"))
        advanceUntilIdle()

        // Assert
        assertThat(model.sortTabIds()).containsExactly(MarketCap.id, TopGainers.id, TopLosers.id).inOrder()
    }

    @Test
    fun `GIVEN a category applied WHEN the filter is cleared THEN buyers is offered again`() = runTest {
        // Arrange
        val model = createModel(testScope = this)
        advanceUntilIdle()
        filterController.onSelectionApplied(selectionOf(id = "layer-1", name = "Layer 1"))
        advanceUntilIdle()

        // Act
        filterController.onSelectionApplied(MarketCategorySelection())
        advanceUntilIdle()

        // Assert
        assertThat(model.sortTabIds()).contains(ExperiencedBuyers.id)
    }

    @Test
    fun `GIVEN sorted by buyers WHEN a category is applied THEN the list is refetched once by market cap`() =
        runTest {
            // Arrange
            val model = createModel(testScope = this)
            advanceUntilIdle()
            model.sortTab(ExperiencedBuyers).onClick()
            advanceUntilIdle()
            actions.clear()
            sentEvents.clear()

            // Act
            filterController.onSelectionApplied(selectionOf(id = "layer-1", name = "Layer 1"))
            advanceUntilIdle()

            // Assert — the fallback is not the user's pick, so no sort by is reported for it
            assertThat(actions.reloadParams().map { it.order to it.geckoCategoryId })
                .containsExactly(TokenMarketListConfig.Order.ByRating to "layer-1")
            assertThat(model.sortTab(MarketCap).isSelected).isTrue()
            assertThat(sectionEvents().filterIsInstance<FeedSectionAnalyticsEvent.SortBy>()).isEmpty()
        }

    @Test
    fun `GIVEN the indicators load WHEN the tab opens THEN the block shows them`() = runTest {
        // Arrange
        coEvery { getMarketIndicatorsUseCase(any()) } returns indicators().right()

        // Act
        val model = createModel(testScope = this)
        advanceUntilIdle()

        // Assert
        val block = model.totalMarketCap() as TotalMarketCapUM.Content
        assertThat(block.indexes.map { it.id }).containsExactly("fear_greed", "altcoin_index").inOrder()
    }

    @Test
    fun `GIVEN the indicators fail to load WHEN the tab opens THEN the block is left out of the list`() = runTest {
        // Arrange
        coEvery { getMarketIndicatorsUseCase(any()) } returns RuntimeException("api down").left()

        // Act
        val model = createModel(testScope = this)
        advanceUntilIdle()

        // Assert
        assertThat(model.totalMarketCap()).isEqualTo(TotalMarketCapUM.Empty)
    }

    @Test
    fun `GIVEN the backend published no indicators WHEN the tab opens THEN the block is left out of the list`() =
        runTest {
            // Arrange — the test environment answers with every field null, which is not a failure
            coEvery { getMarketIndicatorsUseCase(any()) } returns noIndicators.right()

            // Act
            val model = createModel(testScope = this)
            advanceUntilIdle()

            // Assert
            assertThat(model.totalMarketCap()).isEqualTo(TotalMarketCapUM.Empty)
        }

    @Test
    fun `GIVEN loaded indicators WHEN the app currency changes THEN they are refetched in it`() = runTest {
        // Arrange — the backend converts the capitalisation, so a currency switch has to go back to it
        val appCurrency = MutableStateFlow(AppCurrency.Default.right())
        every { getSelectedAppCurrencyUseCase() } returns appCurrency
        coEvery { getMarketIndicatorsUseCase(any()) } returns indicators().right()
        val model = createModel(testScope = this)
        advanceUntilIdle()
        val inUsd = model.headline()

        // Act
        appCurrency.value = euro.right()
        advanceUntilIdle()

        // Assert — asserting on the symbol rather than the whole string keeps this off JDK
        // locale-formatting details
        assertThat(inUsd).contains("\$")
        assertThat(model.headline()).contains("€")
        coVerifyOrder {
            getMarketIndicatorsUseCase(AppCurrency.Default)
            getMarketIndicatorsUseCase(euro)
        }
    }

    @Test
    fun `GIVEN loaded indicators WHEN the app currency changes THEN the old amount never shows the new symbol`() =
        runTest {
            // Arrange — the amount is converted server-side, so it only means anything next to its own currency
            val slowResponseMillis = 1_000L
            val appCurrency = MutableStateFlow(AppCurrency.Default.right())
            every { getSelectedAppCurrencyUseCase() } returns appCurrency
            coEvery { getMarketIndicatorsUseCase(AppCurrency.Default) } returns indicators().right()
            coEvery { getMarketIndicatorsUseCase(euro) } coAnswers {
                delay(slowResponseMillis)
                indicators().copy(totalMarketCap = BigDecimal("0.46")).right()
            }
            val model = createModel(testScope = this)
            advanceUntilIdle()
            val inUsd = model.headline()

            // Act — read the block while the refetch is still in flight
            appCurrency.value = euro.right()
            advanceTimeBy(slowResponseMillis / 2)
            runCurrent()
            val whileRefetching = model.headline()
            advanceUntilIdle()

            // Assert
            assertThat(inUsd).contains("$")
            assertThat(whileRefetching).isEqualTo(inUsd)
            assertThat(model.headline()).contains("€")
        }

    @Test
    fun `GIVEN loaded indicators WHEN a refresh fails THEN the readings stay on screen`() = runTest {
        // Arrange — only a failed first load empties the block
        val appCurrency = MutableStateFlow(AppCurrency.Default.right())
        every { getSelectedAppCurrencyUseCase() } returns appCurrency
        coEvery { getMarketIndicatorsUseCase(any()) } returnsMany listOf(
            indicators().right(),
            RuntimeException("api down").left(),
        )
        val model = createModel(testScope = this)
        advanceUntilIdle()

        // Act — a currency switch is the only refresh the tab makes without waiting for its quotes timer
        appCurrency.value = euro.right()
        advanceUntilIdle()

        // Assert — the refresh has to have happened for the block to be worth asserting on
        coVerify(exactly = 2) { getMarketIndicatorsUseCase(any()) }
        assertThat(model.totalMarketCap()).isInstanceOf(TotalMarketCapUM.Content::class.java)
    }

    @Test
    fun `GIVEN the tab opens WHEN nothing is tapped THEN no sheet is opened`() = runTest {
        // Act
        val model = createModel(testScope = this)
        val openedSheets = model.bottomSheetNavigation.trackSlot()
        advanceUntilIdle()

        // Assert
        assertThat(openedSheets).isEmpty()
    }

    @Test
    fun `GIVEN loaded indicators WHEN a card is tapped THEN its info sheet is opened`() = runTest {
        // Arrange
        coEvery { getMarketIndicatorsUseCase(any()) } returns indicators().right()
        val model = createModel(testScope = this)
        val openedSheets = model.bottomSheetNavigation.trackSlot()
        advanceUntilIdle()
        val block = model.totalMarketCap() as TotalMarketCapUM.Content
        val fearGreed = block.indexes.first()

        // Act
        block.onIndexClick(fearGreed)

        // Assert
        assertThat(openedSheets).containsExactly(FeedCryptoTabBottomSheetConfig.MarketIndexInfo(fearGreed))
    }

    @Test
    fun `GIVEN the headline WHEN its info icon is tapped THEN the market info sheet is opened`() = runTest {
        // Arrange
        coEvery { getMarketIndicatorsUseCase(any()) } returns indicators().right()
        val model = createModel(testScope = this)
        val openedSheets = model.bottomSheetNavigation.trackSlot()
        advanceUntilIdle()

        // Act
        (model.totalMarketCap() as TotalMarketCapUM.Content).onInfoClick()

        // Assert
        assertThat(openedSheets).containsExactly(FeedCryptoTabBottomSheetConfig.MarketInfo)
    }

    private fun CryptoFeedTabModel.totalMarketCap(): TotalMarketCapUM = uiState.value.totalMarketCap

    private fun CryptoFeedTabModel.headline(): String =
        ((totalMarketCap() as TotalMarketCapUM.Content).value as TextReference.Str).value

    /** Mirrors what a real `childSlot` would track, since bare `SlotNavigation` only relays events */
    private fun SlotNavigation<FeedCryptoTabBottomSheetConfig>.trackSlot(): List<FeedCryptoTabBottomSheetConfig?> {
        val tracked = mutableListOf<FeedCryptoTabBottomSheetConfig?>()
        subscribe { event -> tracked.add(event.transformer(tracked.lastOrNull())) }
        return tracked
    }

    @Test
    fun `GIVEN a category deeplink WHEN the tab opens THEN the list is requested once, already filtered`() = runTest {
        // Arrange
        pendingFilter.value = FeedEntryFilter(tabId = FeedTabId.Crypto, categoryId = "layer-1")
        coEvery { categoryResolver.resolve(categoryId = "layer-1", currency = any()) } returns
            MarketCategorySelection.Entry(id = "layer-1", name = "Layer 1")

        // Act
        val model = createModel(testScope = this)
        advanceUntilIdle()

        // Assert - loading unfiltered first would show the wrong list and then swap it out
        assertThat(actions.reloadCategoryIds()).containsExactly("layer-1")
        assertThat((model.chip() as TangemFilterItemUM.Active).value).isEqualTo(TextReference.Str("Layer 1"))
        // the host reports the open; the tab must not also report a pick the user never made
        assertThat(sectionEvents()).isEmpty()
    }

    @Test
    fun `GIVEN a deeplink applied the filter WHEN the user then picks one THEN only that pick is reported`() =
        runTest {
            // Arrange
            pendingFilter.value = FeedEntryFilter(tabId = FeedTabId.Crypto, categoryId = "layer-1")
            coEvery { categoryResolver.resolve(categoryId = "layer-1", currency = any()) } returns
                MarketCategorySelection.Entry(id = "layer-1", name = "Layer 1")
            createModel(testScope = this)
            advanceUntilIdle()

            // Act
            filterController.onSelectionApplied(selectionOf(id = "meme", name = "Meme"))
            advanceUntilIdle()

            // Assert
            assertThat(sectionEvents()).containsExactly(
                FeedSectionAnalyticsEvent.SelectCategory(section = "Crypto", categoryId = "meme"),
            )
        }

    @Test
    fun `GIVEN a category deeplink nothing matches WHEN the tab opens THEN it loads unfiltered once`() = runTest {
        // Arrange
        pendingFilter.value = FeedEntryFilter(tabId = FeedTabId.Crypto, categoryId = "made-up")
        coEvery { categoryResolver.resolve(categoryId = "made-up", currency = any()) } returns null

        // Act
        val model = createModel(testScope = this)
        advanceUntilIdle()

        // Assert
        assertThat(actions.reloadCategoryIds()).containsExactly(null)
        assertThat(model.chip()).isInstanceOf(TangemFilterItemUM.Inactive::class.java)
    }

    @Test
    fun `GIVEN a deeplink for another tab WHEN the tab opens THEN it is left alone and the list is unfiltered`() =
        runTest {
            // Arrange
            pendingFilter.value = FeedEntryFilter(tabId = FeedTabId.Earn, earnType = PreselectedEarnType.Staking)

            // Act
            createModel(testScope = this)
            advanceUntilIdle()

            // Assert
            assertThat(actions.reloadCategoryIds()).containsExactly(null)
            assertThat(pendingFilter.value).isNotNull()
            coVerify(exactly = 0) { categoryResolver.resolve(any(), any()) }
        }

    /** The capitalisation stays below `1` so `compact()` keeps off `android.icu`, which unit tests lack */
    private fun indicators() = MarketIndicators(
        totalMarketCap = BigDecimal("0.5"),
        marketCapChange24h = BigDecimal("-0.0093"),
        fearGreed = MarketIndicators.Index(value = 73, classification = "Greed"),
        altcoinSeason = MarketIndicators.Index(value = 45, classification = "Balanced Market"),
    )

    private val euro = AppCurrency(code = "EUR", name = "Euro", symbol = "€")

    private val noIndicators = MarketIndicators(
        totalMarketCap = null,
        marketCapChange24h = null,
        fearGreed = null,
        altcoinSeason = null,
    )

    @Test
    fun `GIVEN the tab is built WHEN the user touches nothing THEN nothing is reported`() = runTest {
        // Act — every tab model is built when the feed opens, including ones never visited
        createModel(testScope = this)
        advanceUntilIdle()

        // Assert
        assertThat(sectionEvents()).isEmpty()
    }

    @Test
    fun `GIVEN the tab open WHEN the sort changes THEN sort by is reported for the crypto section`() = runTest {
        // Arrange
        val model = createModel(testScope = this)
        advanceUntilIdle()

        // Act
        model.sortTab(TopGainers).onClick()
        advanceUntilIdle()

        // Assert
        assertThat(sectionEvents()).containsExactly(
            FeedSectionAnalyticsEvent.SortBy(
                section = "Crypto",
                type = FeedSortType.Gainers,
                period = FeedSortPeriod.H24,
            ),
        )
    }

    @Test
    fun `GIVEN sorted by gainers WHEN gainers is tapped again THEN sort by is not reported again`() = runTest {
        // Arrange
        val model = createModel(testScope = this)
        advanceUntilIdle()
        model.sortTab(TopGainers).onClick()
        advanceUntilIdle()
        sentEvents.clear()

        // Act
        model.sortTab(TopGainers).onClick()
        model.intervalMenu().onOptionClick(model.intervalMenu().selected)
        advanceUntilIdle()

        // Assert
        assertThat(sectionEvents()).isEmpty()
    }

    @Test
    fun `GIVEN the tab open WHEN the interval changes THEN sort by is reported with the new period`() = runTest {
        // Arrange
        val model = createModel(testScope = this)
        advanceUntilIdle()

        // Act
        model.intervalMenu().onOptionClick(MarketPulseInterval.M1)
        advanceUntilIdle()

        // Assert — the default sort still travels with it
        assertThat(sectionEvents()).containsExactly(
            FeedSectionAnalyticsEvent.SortBy(
                section = "Crypto",
                type = FeedSortType.Rating,
                period = FeedSortPeriod.M1,
            ),
        )
    }

    @Test
    fun `GIVEN the tab open WHEN a category is applied THEN select category carries its stable id`() = runTest {
        // Arrange
        createModel(testScope = this)
        advanceUntilIdle()

        // Act
        filterController.onSelectionApplied(selectionOf(id = "layer-1", name = "Layer 1"))
        advanceUntilIdle()

        // Assert
        assertThat(sectionEvents()).containsExactly(
            FeedSectionAnalyticsEvent.SelectCategory(section = "Crypto", categoryId = "layer-1"),
        )
    }

    @Test
    fun `GIVEN a category applied WHEN the filter is cleared THEN select category reports All`() = runTest {
        // Arrange
        createModel(testScope = this)
        advanceUntilIdle()
        filterController.onSelectionApplied(selectionOf(id = "layer-1", name = "Layer 1"))
        advanceUntilIdle()
        sentEvents.clear()

        // Act
        filterController.onSelectionApplied(MarketCategorySelection())
        advanceUntilIdle()

        // Assert
        assertThat(sectionEvents()).containsExactly(
            FeedSectionAnalyticsEvent.SelectCategory(section = "Crypto", categoryId = "All"),
        )
    }

    @Test
    fun `GIVEN reported events WHEN inspected THEN they carry the agreed params`() = runTest {
        // Arrange
        val model = createModel(testScope = this)
        advanceUntilIdle()

        // Act
        model.sortTab(TopLosers).onClick()
        filterController.onSelectionApplied(selectionOf(id = "meme", name = "Meme"))
        advanceUntilIdle()

        // Assert
        val sortBy = sectionEvents().first { it is FeedSectionAnalyticsEvent.SortBy }
        assertThat(sortBy.id).isEqualTo("[Markets] Sort By")
        assertThat(sortBy.params).containsExactlyEntriesIn(
            mapOf("Section" to "Crypto", "Type" to "Losers", "Period" to "24h"),
        )

        val selectCategory = sectionEvents().first { it is FeedSectionAnalyticsEvent.SelectCategory }
        assertThat(selectCategory.id).isEqualTo("[Markets] Select Category")
        assertThat(selectCategory.params).containsExactlyEntriesIn(
            mapOf("Section" to "Crypto", "Category" to "meme"),
        )
    }

    private fun sectionEvents() = sentEvents.filterIsInstance<FeedSectionAnalyticsEvent>()

    private fun selectionOf(id: String, name: String) = MarketCategorySelection(
        category = MarketCategorySelection.Entry(id = id, name = name),
    )

    private fun CryptoFeedTabModel.chip(): TangemFilterItemUM? = uiState.value.marketPulse.categoriesFilter

    private fun CryptoFeedTabModel.intervalMenu(): MarketPulseIntervalUM = uiState.value.marketPulse.intervalMenu

    /** The tab carries its own `onClick`, so the enum's id is the only bridge from a sort to its tab. */
    private fun CryptoFeedTabModel.sortTab(category: MarketPulseCategory): TangemTabItemUM.Content = uiState
        .value
        .marketPulse
        .sortTabs
        .filterIsInstance<TangemTabItemUM.Content>()
        .first { it.id == category.id }

    private fun CryptoFeedTabModel.sortTabIds(): List<String> = uiState.value.marketPulse.sortTabs
        .filterIsInstance<TangemTabItemUM.Content>()
        .map { it.id }

    private fun List<BatchAction<Int, TokenMarketListConfig, TokenMarketUpdateRequest>>.reloadCount() =
        count { it is BatchAction.Reload<*> }

    private fun List<BatchAction<Int, TokenMarketListConfig, TokenMarketUpdateRequest>>.reloadCategoryIds() =
        reloadParams().map { it.geckoCategoryId }

    private fun List<BatchAction<Int, TokenMarketListConfig, TokenMarketUpdateRequest>>.reloadParams() =
        filterIsInstance<BatchAction.Reload<TokenMarketListConfig>>().map { it.requestParams }

    private fun createModel(testScope: TestScope): CryptoFeedTabModel {
        val testDispatcher = StandardTestDispatcher(testScope.testScheduler)
        every { getMarketsTokenListFlowUseCase(any(), any()) } answers {
            val context = firstArg<TokenListBatchingContext>()
            // undispatched: actionsFlow is a rendezvous SharedFlow and drops anything emitted before a
            // subscriber exists — the model reloads inside its own init
            context.coroutineScope.launch(start = CoroutineStart.UNDISPATCHED) {
                context.actionsFlow.collect(actions::add)
            }
            FakeBatchFlow()
        }

        return CryptoFeedTabModel(
            dispatchers = TestingCoroutineDispatcherProvider(
                main = testDispatcher,
                mainImmediate = testDispatcher,
                io = testDispatcher,
                default = testDispatcher,
                single = testDispatcher,
            ),
            getMarketsTokenListFlowUseCase = getMarketsTokenListFlowUseCase,
            getSelectedAppCurrencyUseCase = getSelectedAppCurrencyUseCase,
            getMarketIndicatorsUseCase = getMarketIndicatorsUseCase,
            categoryFilterController = filterController,
            categoryResolver = categoryResolver,
            feedEntryController = feedEntryController,
            router = router,
            urlOpener = urlOpener,
            analyticsEventHandler = analyticsEventHandler,
        ).also { model = it }
    }

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

    private class FakeBatchFlow : TokenListBatchFlow {
        override val state: StateFlow<BatchListState<Int, List<TokenMarket>>> = MutableStateFlow(
            BatchListState(data = emptyList(), status = PaginationStatus.None),
        )
        override val updateResults:
            SharedFlow<Pair<TokenMarketUpdateRequest, BatchUpdateResult<Int, List<TokenMarket>>>> =
            MutableSharedFlow()
    }
}