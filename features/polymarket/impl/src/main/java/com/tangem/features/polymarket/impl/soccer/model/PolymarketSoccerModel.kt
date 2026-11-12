package com.tangem.features.polymarket.impl.soccer.model

import com.arkivanov.decompose.router.slot.SlotNavigation
import com.arkivanov.decompose.router.slot.activate
import com.tangem.core.decompose.di.ModelScoped
import com.tangem.core.decompose.model.Model
import com.tangem.core.decompose.model.ParamsContainer
import com.tangem.core.decompose.navigation.Router
import com.tangem.core.decompose.ui.UiMessageSender
import com.tangem.core.navigation.url.UrlOpener
import com.tangem.core.ui.extensions.resourceReference
import com.tangem.core.ui.message.SnackbarMessage
import com.tangem.domain.polymarket.model.PolymarketEvent
import com.tangem.domain.polymarket.model.PolymarketEventError
import com.tangem.domain.polymarket.usecase.GetPolymarketEventUseCase
import com.tangem.domain.polymarket.model.PolymarketSoccerEvent
import com.tangem.domain.polymarket.model.PolymarketSportsState
import com.tangem.domain.polymarket.usecase.ObservePolymarketSportsStateUseCase
import com.tangem.domain.polymarket.model.PolymarketSoccerTab
import com.tangem.domain.polymarket.usecase.GetPolymarketPriceHistoryUseCase
import com.tangem.features.polymarket.impl.details.model.chart.PolymarketEventChartController
import com.tangem.utils.time.CurrentTimeProvider
import com.tangem.domain.polymarket.usecase.GetPredictionPricesUseCase
import com.tangem.domain.polymarket.usecase.ObservePolymarketLiveQuotesUseCase
import com.tangem.domain.polymarket.usecase.GetPolymarketSoccerEventUseCase
import com.tangem.features.polymarket.api.PolymarketFeatureToggles
import com.tangem.features.polymarket.impl.R
import com.tangem.features.polymarket.impl.common.feed.model.PolymarketStaleDataReporter
import com.tangem.features.polymarket.impl.common.region.PredictionRegionAccess
import com.tangem.features.polymarket.impl.common.region.regionRestrictionsMessage
import com.tangem.features.polymarket.impl.navigation.PolymarketRoute
import com.tangem.features.polymarket.impl.soccer.PolymarketSoccerComponent
import com.tangem.features.polymarket.impl.soccer.ui.state.PolymarketSoccerCardUM
import com.tangem.features.polymarket.impl.soccer.ui.state.PolymarketSoccerUM
import com.tangem.utils.coroutines.CoroutineDispatcherProvider
import com.tangem.utils.logging.TangemLogger
import com.tangem.utils.coroutines.JobHolder
import com.tangem.utils.coroutines.saveIn
import kotlinx.coroutines.delay
import kotlinx.coroutines.ensureActive
import kotlin.coroutines.coroutineContext
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.filterNotNull
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import javax.inject.Inject
import java.time.Instant
import kotlin.time.Duration.Companion.seconds

@ModelScoped
@Suppress("LongParameterList")
internal class PolymarketSoccerModel @Inject constructor(
    paramsContainer: ParamsContainer,
    override val dispatchers: CoroutineDispatcherProvider,
    private val getEvent: GetPolymarketSoccerEventUseCase,
    private val getMainEvent: GetPolymarketEventUseCase,
    getPrices: GetPredictionPricesUseCase,
    observeQuotes: ObservePolymarketLiveQuotesUseCase,
    observeSports: ObservePolymarketSportsStateUseCase,
    getHistory: GetPolymarketPriceHistoryUseCase,
    currentTimeProvider: CurrentTimeProvider,
    private val router: Router,
    private val messageSender: UiMessageSender,
    private val regionAccess: PredictionRegionAccess,
    private val urlOpener: UrlOpener,
    private val toggles: PolymarketFeatureToggles,
) : Model() {
    private val params = paramsContainer.require<PolymarketSoccerComponent.Params>()
    private val loadingJob = JobHolder()
    private val converter = PolymarketSoccerConverter(::onTabClick)
    private var event: PolymarketSoccerEvent? = null
    private var selection = PolymarketSoccerSelection()
    private val isResumed = MutableStateFlow(false)
    private val sportsEvents = MutableStateFlow<PolymarketSoccerEvent?>(null)
    private val mainEvents = MutableStateFlow<PolymarketEvent?>(null)
    private var isAwaitingFinalSnapshot = false
    private var sportsGame: PolymarketSportsState.Game? = null
    private var resnapshotRevision = 0L
    private var isStaleReported = false
    private var isDescriptionExpanded = false
    private var isChartResumed = false

    private val staleReporter = PolymarketStaleDataReporter(::showStaleMessage)
    private val prices = PolymarketSoccerPrices(
        scope = modelScope,
        getPrices = getPrices,
        observeQuotes = observeQuotes,
        isEnabled = toggles.isPolymarketPhaseTwoEnabled,
        onStale = staleReporter::report,
        onQuoteRecovery = ::load,
    )

    private val chart = PolymarketEventChartController(
        scope = modelScope,
        getHistory = getHistory,
        currentTimeProvider = currentTimeProvider,
        isEnabled = toggles.isPolymarketPhaseTwoEnabled,
    )
    val chartState = chart.state

    val rulesNavigation = SlotNavigation<PolymarketSoccerCardUM.Rules>()

    val uiState: StateFlow<PolymarketSoccerUM>
        field = MutableStateFlow<PolymarketSoccerUM>(PolymarketSoccerUM.Loading)

    init {
        if (toggles.isPolymarketPhaseTwoEnabled) {
            modelScope.launch {
                observeSports.observeSoccer(
                    sportsEvents.filterNotNull(),
                    isResumed,
                    mainEvents.filterNotNull(),
                ).collect(::updateSports)
            }
        }
        modelScope.launch { prices.liveQuotes.collect { updateChart() } }
        modelScope.launch {
            prices.state.collect { update ->
                val content = uiState.value as? PolymarketSoccerUM.Content ?: return@collect
                uiState.value = content.withPrices(update)
            }
        }
    }

    fun onResume() {
        if (isResumed.value) return
        isResumed.value = true
        load()
    }

    fun onPause() {
        isResumed.value = false
        loadingJob.cancel()
        prices.onPause()
        chart.onPause()
        isChartResumed = false
    }

    fun onRetry() = load()

    fun onChartPeriodChange(id: String) = chart.selectPeriod(id)

    fun onBack() = router.pop()

    fun onOpenUrl(url: String) = urlOpener.openUrl(url)

    fun onReadMore() {
        isDescriptionExpanded = true
        showContent()
    }

    fun onRules(rules: PolymarketSoccerCardUM.Rules) {
        rulesNavigation.activate(rules)
    }

    fun onPositionChange(cardId: String, marketId: String) {
        val loaded = event ?: return
        selection = selection.selectPosition(loaded, cardId, marketId)
        showContent()
    }

    fun onOutcomeClick(target: PolymarketSoccerCardUM.Target) {
        if (regionAccess.isRestricted) return messageSender.send(regionRestrictionsMessage())
        router.push(
            PolymarketRoute.PlacePrediction(
                userWalletId = params.userWalletId,
                eventId = target.eventId,
                marketId = target.marketId,
                assetId = target.assetId,
            ),
        )
    }

    private fun onTabClick(tab: PolymarketSoccerTab.Key) {
        val loaded = event ?: return
        selection = selection.selectTab(loaded, tab)
        showContent()
    }

    private fun load() {
        if (!isResumed.value || !toggles.isPolymarketPhaseTwoEnabled) return
        modelScope.launch {
            if (event == null) uiState.value = PolymarketSoccerUM.Loading
            do {
                if (event == null || isAwaitingFinalSnapshot) loadSoccer() else loadMainEvent()
                if (event == null) break
                delay(POLL_INTERVAL)
            } while (true)
        }.saveIn(loadingJob)
    }

    private suspend fun loadSoccer() {
        val result = withContext(dispatchers.default) { getEvent(params.userWalletId, event?.id ?: params.eventId) }
        coroutineContext.ensureActive()
        result.fold(
            ifLeft = ::onRefreshFailed,
            ifRight = { loaded ->
                if (isOutdated(loaded.dataFetchedAt) || isAwaitingFinalSnapshot && loaded.isEnded != true) {
                    onRefreshFailed(PolymarketEventError.Unknown(null, "Outdated soccer snapshot"))
                    return@fold
                }
                event = loaded
                selection = selection.reconcile(loaded)
                sportsEvents.value = loaded
                isAwaitingFinalSnapshot = false
                onRefreshed()
            },
        )
    }

    private suspend fun loadMainEvent() {
        val loaded = event ?: return
        val result = withContext(dispatchers.default) { getMainEvent(params.userWalletId, loaded.id) }
        coroutineContext.ensureActive()
        result.fold(
            ifLeft = ::onRefreshFailed,
            ifRight = { update ->
                if (update.id != loaded.id || update.gameId != loaded.gameId) {
                    onRefreshFailed(PolymarketEventError.Unknown(null, "Mismatched main event"))
                    return@fold
                }
                if (isOutdated(update.dataFetchedAt)) {
                    onRefreshFailed(PolymarketEventError.Unknown(null, "Outdated main event snapshot"))
                    return@fold
                }
                val shouldFetchFinalSnapshot = update.isEnded == true && loaded.isEnded != true
                isAwaitingFinalSnapshot = isAwaitingFinalSnapshot || shouldFetchFinalSnapshot
                mainEvents.value = update
                event = loaded.withMainEvent(update)
                if (shouldFetchFinalSnapshot) {
                    prices.awaitFinalSnapshot()
                    loadSoccer()
                } else {
                    onRefreshed(isRestUpdate = loaded.hasCurrentMoneyline(update))
                }
            },
        )
    }

    private fun isOutdated(timestamp: Instant?): Boolean {
        val previous = listOfNotNull(mainEvents.value?.dataFetchedAt, event?.dataFetchedAt).maxOrNull() ?: return false
        return timestamp == null || timestamp < previous
    }

    private fun onRefreshFailed(error: PolymarketEventError) {
        chart.onRefreshFailed()
        TangemLogger.e("Soccer details: load of ${params.eventId} failed: $error")
        if (event == null) {
            uiState.value = PolymarketSoccerUM.Error
        } else if (!isStaleReported) {
            isStaleReported = true
            staleReporter.report()
        }
    }

    private fun onRefreshed(isRestUpdate: Boolean = true) {
        val loaded = event ?: return
        isStaleReported = false
        prices.update(loaded.withGamePhase())
        prices.onResume()
        updateChart(isRestUpdate = isRestUpdate)
        if (!isChartResumed) {
            isChartResumed = true
            chart.onResume()
        }
        showContent()
    }

    private fun showContent() {
        val loaded = event ?: return
        uiState.value = converter.convert(loaded, selection).copy(
            isDescriptionExpanded = isDescriptionExpanded,
            sports = loaded.sportsUM(sportsGame),
        ).withPrices(prices.state.value)
    }

    private fun updateSports(state: PolymarketSportsState) {
        val loaded = event ?: return
        val previous = sportsGame
        val game = state.events[loaded.id]?.takeIf { it.gameId == loaded.gameId }
        sportsGame = game
        val hasJustEnded = game?.isEnded == true && previous?.isEnded != true
        val shouldRequestFinalSnapshot = hasJustEnded && loaded.isEnded != true && !isAwaitingFinalSnapshot
        if (hasJustEnded && loaded.isEnded != true) {
            isAwaitingFinalSnapshot = true
            prices.awaitFinalSnapshot()
        } else {
            if (game?.isEnded == false && previous?.isEnded == true) isAwaitingFinalSnapshot = false
            prices.update(
                loaded.withGamePhase(),
                hasReopened = previous?.isEnded == true && game?.isEnded == false,
            )
        }
        val content = uiState.value as? PolymarketSoccerUM.Content
        if (content != null) uiState.value = content.copy(sports = loaded.sportsUM(game))
        if (game?.isStale == true) staleReporter.report()
        val shouldRefresh = state.resnapshotRevision > resnapshotRevision || shouldRequestFinalSnapshot
        resnapshotRevision = state.resnapshotRevision
        updateChart()
        if (shouldRefresh) load()
    }

    private fun PolymarketSoccerEvent.withGamePhase(): PolymarketSoccerEvent =
        sportsGame?.takeIf { it.gameId == gameId }?.let { copy(isLive = it.isLive) } ?: this

    private fun updateChart(isRestUpdate: Boolean = false) {
        val loaded = event ?: return
        val game = sportsGame?.takeIf { it.gameId == loaded.gameId }
        chart.updateSoccer(
            event = loaded.copy(isLive = game?.isLive ?: loaded.isLive, isEnded = game?.isEnded ?: loaded.isEnded),
            quotes = prices.liveQuotes.value,
            isRestUpdate = isRestUpdate,
        )
    }

    private fun showStaleMessage() {
        messageSender.send(SnackbarMessage(message = resourceReference(R.string.prediction_main_data_outdated)))
    }

    private companion object {
        val POLL_INTERVAL = 10.seconds
    }
}