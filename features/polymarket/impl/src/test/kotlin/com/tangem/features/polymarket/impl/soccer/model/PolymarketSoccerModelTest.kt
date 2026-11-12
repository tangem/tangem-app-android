package com.tangem.features.polymarket.impl.soccer.model

import arrow.core.left
import arrow.core.right
import com.google.common.truth.Truth.assertThat
import com.tangem.core.decompose.model.MutableParamsContainer
import com.tangem.core.decompose.navigation.Router
import com.tangem.core.decompose.ui.UiMessageSender
import com.tangem.core.navigation.url.UrlOpener
import com.tangem.core.ui.ds2.tabnavigation.TangemTabItemUM
import com.tangem.core.ui.extensions.resourceReference
import com.tangem.core.ui.extensions.stringReference
import com.tangem.core.ui.message.SnackbarMessage
import com.tangem.domain.models.wallet.UserWalletId
import com.tangem.domain.polymarket.model.PolymarketEventError
import com.tangem.domain.polymarket.model.PolymarketLiveQuote
import com.tangem.domain.polymarket.model.PolymarketHistoryToken
import com.tangem.domain.polymarket.model.PolymarketPricePoint
import com.tangem.domain.polymarket.model.PolymarketSportsState
import com.tangem.domain.polymarket.usecase.ObservePolymarketSportsStateUseCase
import com.tangem.domain.polymarket.model.PolymarketSoccerEvent
import com.tangem.domain.polymarket.model.PredictionPriceError
import com.tangem.domain.polymarket.model.PolymarketSoccerTab
import com.tangem.domain.polymarket.usecase.GetPolymarketPriceHistoryUseCase
import com.tangem.domain.polymarket.usecase.GetPredictionPricesUseCase
import com.tangem.domain.polymarket.usecase.ObservePolymarketLiveQuotesUseCase
import com.tangem.domain.polymarket.usecase.GetPolymarketSoccerEventUseCase
import com.tangem.features.polymarket.impl.R
import com.tangem.features.polymarket.impl.common.region.regionRestrictionsMessage
import com.tangem.features.polymarket.impl.common.region.PredictionRegionAccess
import com.tangem.features.polymarket.impl.navigation.PolymarketRoute
import com.tangem.features.polymarket.impl.soccer.PolymarketSoccerComponent
import com.tangem.features.polymarket.impl.soccer.ui.state.PolymarketSoccerCardUM
import com.tangem.features.polymarket.impl.soccer.ui.state.PolymarketSoccerUM
import com.tangem.test.core.createTestingCoroutineDispatcherProvider
import io.mockk.coEvery
import io.mockk.coVerify
import io.mockk.every
import io.mockk.mockk
import io.mockk.verify
import kotlinx.coroutines.CompletableDeferred
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.emptyFlow
import kotlinx.coroutines.awaitCancellation
import kotlinx.coroutines.test.TestScope
import kotlinx.coroutines.test.advanceTimeBy
import kotlinx.coroutines.test.runCurrent
import kotlinx.coroutines.test.runTest
import org.junit.jupiter.api.AfterEach
import org.junit.jupiter.api.Test
import java.math.BigDecimal

@OptIn(ExperimentalCoroutinesApi::class)
internal class PolymarketSoccerModelTest {
    private val getEvent: GetPolymarketSoccerEventUseCase = mockk()
    private val getPrices: GetPredictionPricesUseCase = mockk {
        coEvery { this@mockk(any(), any()) } returns emptyMap<String, BigDecimal?>().right()
    }
    private val observeQuotes: ObservePolymarketLiveQuotesUseCase = mockk {
        every { this@mockk(any()) } returns emptyFlow()
    }
    private val sports = MutableStateFlow(PolymarketSportsState(emptyMap()))
    private val observeSports: ObservePolymarketSportsStateUseCase = mockk {
        every { observeSoccer(any(), any()) } returns sports
    }
    private val getHistory: GetPolymarketPriceHistoryUseCase = mockk {
        coEvery { getSoccerHistory(any(), any(), any()) } returns emptyMap()
    }
    private val router: Router = mockk(relaxed = true)
    private val messages: UiMessageSender = mockk(relaxed = true)
    private val region: PredictionRegionAccess = mockk { every { isRestricted } returns false }
    private val urls: UrlOpener = mockk(relaxed = true)
    private val wallet = UserWalletId("011")
    private var model: PolymarketSoccerModel? = null

    @AfterEach
    fun tearDown() { model?.onDestroy() }

    @Test
    fun `GIVEN disabled D2 WHEN resumed and retried THEN soccer endpoint is never called`() = runTest {
        // Arrange
        val model = createModel(enabled = false)

        // Act
        model.onResume()
        model.onRetry()
        advanceTimeBy(30_000)
        runCurrent()

        // Assert
        coVerify(exactly = 0) { getEvent(any(), any()) }
        coVerify(exactly = 0) { getPrices(any(), any()) }
        coVerify(exactly = 0) { getHistory.getSoccerHistory(any(), any(), any()) }
        verify(exactly = 0) { observeQuotes(any()) }
        verify(exactly = 0) { observeSports.observeSoccer(any(), any()) }
        model.onDestroy()
    }

    @Test
    fun `GIVEN active screen WHEN paused and resumed THEN polling stops and refreshes once immediately`() = runTest {
        // Arrange
        coEvery { getEvent(wallet, "child") } returns soccerEvent(emptyList()).right()
        val model = createModel()
        coVerify(exactly = 0) { getEvent(any(), any()) }
        model.onResume()
        model.onResume()
        runCurrent()
        advanceTimeBy(10_000)
        runCurrent()
        coVerify(exactly = 2) { getEvent(wallet, "child") }

        // Act
        model.onPause()
        advanceTimeBy(30_000)
        runCurrent()
        coVerify(exactly = 2) { getEvent(any(), any()) }
        model.onResume()
        runCurrent()

        // Assert
        coVerify(exactly = 3) { getEvent(wallet, "child") }
        assertThat(model.uiState.value).isInstanceOf(PolymarketSoccerUM.Content::class.java)
        model.onDestroy()
    }

    @Test
    fun `GIVEN in-flight request WHEN paused THEN request is cancelled and resume reloads`() = runTest {
        // Arrange
        val cancelled = CompletableDeferred<Unit>()
        coEvery { getEvent(any(), any()) } coAnswers {
            try { awaitCancellation() } finally { cancelled.complete(Unit) }
        }
        val model = createModel()
        model.onResume()
        runCurrent()

        // Act
        model.onPause()
        runCurrent()
        coEvery { getEvent(any(), any()) } returns soccerEvent(emptyList()).right()
        model.onResume()
        runCurrent()

        // Assert
        assertThat(cancelled.isCompleted).isTrue()
        assertThat(model.uiState.value).isInstanceOf(PolymarketSoccerUM.Content::class.java)
        coVerify(exactly = 2) { getEvent(any(), any()) }
        model.onDestroy()
    }

    @Test
    fun `GIVEN not found WHEN retry succeeds THEN error is replaced with content`() = runTest {
        // Arrange
        coEvery { getEvent(any(), any()) } returnsMany listOf(
            PolymarketEventError.NotFound.left(), soccerEvent(emptyList()).right(),
        )
        val model = createModel()
        model.onResume()
        runCurrent()
        assertThat(model.uiState.value).isEqualTo(PolymarketSoccerUM.Error)
        advanceTimeBy(30_000)
        coVerify(exactly = 1) { getEvent(any(), any()) }

        // Act
        model.onRetry()
        runCurrent()

        // Assert
        assertThat(model.uiState.value).isInstanceOf(PolymarketSoccerUM.Content::class.java)
        coVerify(exactly = 2) { getEvent(any(), any()) }
        model.onDestroy()
    }

    @Test
    fun `GIVEN content WHEN refresh fails repeatedly THEN retain content and respect notification cooldown`() = runTest {
        // Arrange
        val event = soccerEvent(emptyList())
        coEvery { getEvent(any(), any()) } returnsMany listOf(
            event.right(), PolymarketEventError.Network.left(), PolymarketEventError.Network.left(),
            event.copy(title = "Updated").right(), PolymarketEventError.Network.left(),
        )
        val model = createModel()
        model.onResume()
        runCurrent()
        val initial = model.uiState.value

        // Act
        repeat(2) { advanceTimeBy(10_000); runCurrent() }

        // Assert
        assertThat(model.uiState.value).isSameInstanceAs(initial)
        verify(exactly = 1) {
            messages.send(SnackbarMessage(message = resourceReference(R.string.prediction_main_data_outdated)))
        }
        repeat(2) { advanceTimeBy(10_000); runCurrent() }
        assertThat(content().title).isEqualTo(stringReference("Updated"))
        verify(exactly = 1) { messages.send(any()) }
        model.onDestroy()
    }

    @Test
    fun `GIVEN ended match WHEN REST refreshes THEN settlement data still updates`() = runTest {
        // Arrange
        coEvery { getEvent(any(), any()) } returnsMany listOf(
            soccerEvent(emptyList()).copy(isEnded = true).right(),
            soccerEvent(emptyList()).copy(isEnded = true, description = "Settled").right(),
        )
        val model = createModel()
        model.onResume()
        runCurrent()

        // Act
        advanceTimeBy(10_000)
        runCurrent()

        // Assert
        assertThat(content().description).isEqualTo(stringReference("Settled"))
        coVerify(exactly = 2) { getEvent(any(), any()) }
        model.onDestroy()
    }

    @Test
    fun `GIVEN selected line and tab WHEN refreshed THEN selection and child order targets survive`() = runTest {
        // Arrange
        val card = soccerPickerCard().let { it.copy(markets = it.markets.map { m -> m.copy(volume = null) }) }
        val event = soccerEvent(listOf(card)).let {
            it.copy(tabs = it.tabs + PolymarketSoccerTab(PolymarketSoccerTab.Key.FIRST_HALF, listOf(card)))
        }
        coEvery { getEvent(any(), any()) } returns event.right()
        val model = createModel()
        model.onResume()
        runCurrent()
        (content().tabs.last() as TangemTabItemUM.Content).onClick()
        model.onPositionChange("first_half/spreads", "p1")
        model.onReadMore()

        // Act
        advanceTimeBy(10_000)
        runCurrent()
        val selected = content().cards.single()
        model.onRules(requireNotNull(selected.rules))
        val picker = selected.content as PolymarketSoccerCardUM.Content.Picker
        model.onOutcomeClick(picker.outcomes.first().target)

        // Assert
        assertThat((content().tabs.last() as TangemTabItemUM.Content).isSelected).isTrue()
        assertThat(picker.selectedId).isEqualTo("p1")
        assertThat(content().isDescriptionExpanded).isTrue()
        assertThat(selected.rules?.description).isEqualTo("Rules p1")
        verify { router.push(PolymarketRoute.PlacePrediction(wallet, "child-p1", "p1", "p1-left")) }
        model.onDestroy()
    }

    @Test
    fun `GIVEN restricted region WHEN outcome tapped THEN order does not open`() = runTest {
        // Arrange
        every { region.isRestricted } returns true
        val model = createModel()

        // Act
        model.onOutcomeClick(PolymarketSoccerCardUM.Target("child", "market", "token"))

        // Assert
        verify(exactly = 0) { router.push(any(), any()) }
        verify(exactly = 1) { messages.send(regionRestrictionsMessage()) }
        model.onDestroy()
    }

    @Test
    fun `GIVEN batched prices WHEN selected position and tab change THEN latest prices survive stale REST`() = runTest {
        // Arrange
        val card = soccerPickerCard().let { it.copy(markets = it.markets.map { m -> m.copy(volume = null) }) }
        val event = soccerEvent(listOf(card)).let {
            it.copy(tabs = it.tabs + PolymarketSoccerTab(PolymarketSoccerTab.Key.FIRST_HALF, listOf(card)))
        }
        coEvery { getEvent(any(), any()) } returns event.right()
        coEvery { getPrices(any(), any()) } returns mapOf("p1-left" to BigDecimal("0.61")).right()
        val model = createModel()
        model.onResume()
        runCurrent()

        // Act
        (content().tabs.last() as TangemTabItemUM.Content).onClick()
        model.onPositionChange("first_half/spreads", "p1")
        advanceTimeBy(10_000)
        runCurrent()

        // Assert
        val picker = content().cards.single().content as PolymarketSoccerCardUM.Content.Picker
        assertThat(picker.selectedId).isEqualTo("p1")
        assertThat(picker.outcomes.first().price).isEqualTo(stringReference("61¢"))
        coVerify(exactly = 2) { getPrices(any(), any()) }
        model.onDestroy()
    }

    @Test
    fun `GIVEN backgrounded live match WHEN resume REST is pending THEN no old phase price requests start`() = runTest {
        // Arrange
        val card = soccerPickerCard().let { it.copy(markets = it.markets.map { m -> m.copy(volume = null) }) }
        val event = soccerEvent(listOf(card))
        coEvery { getEvent(any(), any()) } returns event.right()
        val model = createModel()
        model.onResume()
        runCurrent()
        model.onPause()
        val response = CompletableDeferred<PolymarketSoccerEvent>()
        coEvery { getEvent(any(), any()) } coAnswers { response.await().right() }

        // Act
        model.onResume()
        advanceTimeBy(30_000)
        runCurrent()

        // Assert
        coVerify(exactly = 1) { getPrices(any(), any()) }
        response.complete(event.copy(isEnded = true))
        runCurrent()
        advanceTimeBy(10_000)
        runCurrent()
        coVerify(exactly = 1) { getPrices(any(), any()) }
        model.onDestroy()
    }

    @Test
    fun `GIVEN REST and batch failures WHEN both report stale data THEN one shared notification is shown`() = runTest {
        // Arrange
        val card = soccerPickerCard().let { it.copy(markets = it.markets.map { m -> m.copy(volume = null) }) }
        coEvery { getEvent(any(), any()) } returnsMany listOf(
            soccerEvent(listOf(card)).right(), PolymarketEventError.Network.left(),
        )
        coEvery { getPrices(any(), any()) } returns PredictionPriceError.Network.left()
        val model = createModel()

        // Act
        model.onResume()
        runCurrent()
        advanceTimeBy(20_000)
        runCurrent()

        // Assert
        verify(exactly = 1) {
            messages.send(SnackbarMessage(message = resourceReference(R.string.prediction_main_data_outdated)))
        }
        model.onDestroy()
    }

    @Test
    fun `GIVEN live Sports WS WHEN ended before REST THEN stop prices until final snapshot succeeds`() = runTest {
        // Arrange
        val card = soccerPickerCard().let { it.copy(markets = it.markets.map { m -> m.copy(volume = null) }) }
        val event = soccerEvent(listOf(card))
        coEvery { getEvent(any(), any()) } returnsMany listOf(
            event.right(), PolymarketEventError.Network.left(), event.right(), event.copy(isEnded = true).right(),
        )
        coEvery { getPrices(any(), any()) } returns mapOf("p2-left" to BigDecimal("0.61")).right()
        val model = createModel()
        model.onResume()
        runCurrent()
        val originalCards = content().cards
        val originalTabs = content().tabs

        // Act
        sports.value = PolymarketSportsState(mapOf("main" to soccerGame(event, ended = true)))
        runCurrent()

        // Assert
        assertThat(content().sports.isLive).isFalse()
        assertThat(content().cards).isSameInstanceAs(originalCards)
        assertThat(content().tabs).isSameInstanceAs(originalTabs)
        repeat(2) { advanceTimeBy(10_000); runCurrent() }
        coVerify(exactly = 1) { getPrices(any(), any()) }
        coVerify(exactly = 4) { getEvent(any(), any()) }
        val picker = content().cards.single().content as PolymarketSoccerCardUM.Content.Picker
        assertThat(picker.outcomes.first().price).isEqualTo(stringReference("—"))
        model.onDestroy()
    }

    @Test
    fun `GIVEN reconnect revision WHEN it changes THEN refresh once without needing a score change`() = runTest {
        // Arrange
        coEvery { getEvent(any(), any()) } returns soccerEvent(emptyList()).right()
        val model = createModel()
        model.onResume()
        runCurrent()

        // Act
        sports.value = PolymarketSportsState(emptyMap(), resnapshotRevision = 1)
        runCurrent()
        sports.value = sports.value.copy()
        runCurrent()

        // Assert
        coVerify(exactly = 2) { getEvent(any(), any()) }
        model.onDestroy()
    }

    @Test
    fun `GIVEN unconfirmed socket completion WHEN a newer frame reopens the match THEN prices resume`() = runTest {
        // Arrange
        val card = soccerPickerCard().let { it.copy(markets = it.markets.map { m -> m.copy(volume = null) }) }
        val event = soccerEvent(listOf(card))
        coEvery { getEvent(any(), any()) } returns event.right()
        val model = createModel()
        model.onResume()
        runCurrent()
        sports.value = PolymarketSportsState(mapOf("main" to soccerGame(event, ended = true)))
        runCurrent()
        advanceTimeBy(10_000)
        runCurrent()
        coVerify(exactly = 1) { getPrices(any(), any()) }

        // Act
        sports.value = PolymarketSportsState(mapOf("main" to soccerGame(event, ended = false)))
        runCurrent()
        advanceTimeBy(10_000)
        runCurrent()

        // Assert
        assertThat(content().sports.isLive).isTrue()
        coVerify(exactly = 3) { getPrices(any(), any()) }
        model.onDestroy()
    }

    @Test
    fun `GIVEN live chart history failure WHEN quotes and REST update THEN retry only on REST and resume after fresh snapshot`() = runTest {
        // Arrange
        val card = threeWayCard().let { it.copy(markets = it.markets.map { m -> m.copy(volume = null) }) }
        val event = soccerEvent(listOf(card))
        val quotes = MutableStateFlow(emptyMap<String, PolymarketLiveQuote>())
        every { observeQuotes(any()) } returns quotes
        coEvery { getEvent(any(), any()) } returns event.right()
        val model = createModel()
        model.onResume()
        runCurrent()
        coVerify(exactly = 1) { getHistory.getSoccerHistory(any(), any(), any()) }

        // Act
        repeat(3) { index ->
            quotes.value = mapOf("left-yes" to PolymarketLiveQuote(
                "left-yes", bestBid = BigDecimal("0.4"), bestAsk = BigDecimal("0.44"),
                updatedAtMillis = index.toLong(), status = PolymarketLiveQuote.Status.LIVE,
            ))
            advanceTimeBy(300)
            runCurrent()
        }

        // Assert
        coVerify(exactly = 1) { getHistory.getSoccerHistory(any(), any(), any()) }
        advanceTimeBy(9_100)
        runCurrent()
        coVerify(exactly = 2) { getHistory.getSoccerHistory(any(), any(), any()) }
        verify(exactly = 1) { observeQuotes(any()) }
        model.onPause()
        val response = CompletableDeferred<PolymarketSoccerEvent>()
        coEvery { getEvent(any(), any()) } coAnswers { response.await().right() }
        model.onResume()
        advanceTimeBy(30_000)
        runCurrent()
        coVerify(exactly = 2) { getHistory.getSoccerHistory(any(), any(), any()) }
        response.complete(event)
        runCurrent()
        coVerify(exactly = 3) { getHistory.getSoccerHistory(any(), any(), any()) }
        model.onDestroy()
    }

    @Test
    fun `GIVEN loaded chart WHEN structural refresh fails THEN keep history visible`() = runTest {
        // Arrange
        val card = threeWayCard().let { it.copy(markets = it.markets.map { m -> m.copy(volume = null) }) }
        coEvery { getEvent(any(), any()) } returnsMany listOf(
            soccerEvent(listOf(card)).right(), PolymarketEventError.Network.left(),
        )
        coEvery { getHistory.getSoccerHistory(any(), any(), any()) } coAnswers {
            secondArg<Set<PolymarketHistoryToken>>().associateWith { listOf(
                PolymarketPricePoint(1_789_999_880_000, BigDecimal("0.4")),
                PolymarketPricePoint(1_789_999_940_000, BigDecimal("0.5")),
            ).right() }
        }
        val model = createModel()
        model.onResume()
        runCurrent()
        val chart = model.chartState.value!!.chart

        // Act
        advanceTimeBy(10_000)
        runCurrent()

        // Assert
        assertThat(model.chartState.value!!.chart).isEqualTo(chart)
        coVerify(exactly = 1) { getHistory.getSoccerHistory(any(), any(), any()) }
        model.onDestroy()
    }

    private fun soccerGame(event: PolymarketSoccerEvent, ended: Boolean) = PolymarketSportsState.Game(
        gameId = requireNotNull(event.gameId), score = PolymarketSportsState.Score(2, 1),
        period = if (ended) "FT" else "2H", isLive = !ended, isEnded = ended,
        home = PolymarketSportsState.Team(event.teams[0], 0), away = PolymarketSportsState.Team(event.teams[1], 1),
        isStale = false,
    )

    private fun TestScope.createModel(enabled: Boolean = true): PolymarketSoccerModel = PolymarketSoccerModel(
        paramsContainer = MutableParamsContainer(PolymarketSoccerComponent.Params("child", wallet)),
        dispatchers = createTestingCoroutineDispatcherProvider(),
        getEvent = getEvent,
        getPrices = getPrices,
        observeQuotes = observeQuotes,
        observeSports = observeSports,
        getHistory = getHistory,
        currentTimeProvider = { 1_790_000_000_000L + testScheduler.currentTime },
        router = router,
        messageSender = messages,
        regionAccess = region,
        urlOpener = urls,
        toggles = mockk { every { isPolymarketPhaseTwoEnabled } returns enabled },
    ).also { model = it }

    private fun content() = model!!.uiState.value as PolymarketSoccerUM.Content
}