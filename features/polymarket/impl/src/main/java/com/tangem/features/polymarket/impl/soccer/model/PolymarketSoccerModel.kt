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
import com.tangem.domain.polymarket.model.PolymarketSoccerEvent
import com.tangem.domain.polymarket.model.PolymarketSoccerTab
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
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import javax.inject.Inject
import kotlin.time.Duration.Companion.seconds

@ModelScoped
@Suppress("LongParameterList")
internal class PolymarketSoccerModel @Inject constructor(
    paramsContainer: ParamsContainer,
    override val dispatchers: CoroutineDispatcherProvider,
    private val getEvent: GetPolymarketSoccerEventUseCase,
    getPrices: GetPredictionPricesUseCase,
    observeQuotes: ObservePolymarketLiveQuotesUseCase,
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
    private var isResumed = false
    private var isStaleReported = false
    private var isDescriptionExpanded = false

    private val staleReporter = PolymarketStaleDataReporter(::showStaleMessage)
    private val prices = PolymarketSoccerPrices(
        scope = modelScope,
        getPrices = getPrices,
        observeQuotes = observeQuotes,
        isEnabled = toggles.isPolymarketPhaseTwoEnabled,
        onStale = staleReporter::report,
        onQuoteRecovery = ::load,
    )

    val rulesNavigation = SlotNavigation<PolymarketSoccerCardUM.Rules>()

    val uiState: StateFlow<PolymarketSoccerUM>
        field = MutableStateFlow<PolymarketSoccerUM>(PolymarketSoccerUM.Loading)

    init {
        modelScope.launch {
            prices.state.collect { update ->
                val content = uiState.value as? PolymarketSoccerUM.Content ?: return@collect
                uiState.value = content.withPrices(update)
            }
        }
    }

    fun onResume() {
        if (isResumed) return
        isResumed = true
        load()
    }

    fun onPause() {
        isResumed = false
        loadingJob.cancel()
        prices.onPause()
    }

    fun onRetry() = load()

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
        if (!isResumed || !toggles.isPolymarketPhaseTwoEnabled) return
        modelScope.launch {
            if (event == null) uiState.value = PolymarketSoccerUM.Loading
            do {
                val result = withContext(dispatchers.default) { getEvent(params.userWalletId, params.eventId) }
                result.fold(
                    ifLeft = { error ->
                        TangemLogger.e("Soccer details: load of ${params.eventId} failed: $error")
                        if (event == null) {
                            uiState.value = PolymarketSoccerUM.Error
                        } else if (!isStaleReported) {
                            isStaleReported = true
                            staleReporter.report()
                        }
                    },
                    ifRight = { loaded ->
                        event = loaded
                        selection = selection.reconcile(loaded)
                        isStaleReported = false
                        prices.update(loaded)
                        prices.onResume()
                        showContent()
                    },
                )
                if (event == null) break
                delay(POLL_INTERVAL)
            } while (true)
        }.saveIn(loadingJob)
    }

    private fun showContent() {
        val loaded = event ?: return
        uiState.value = converter.convert(loaded, selection).copy(
            isDescriptionExpanded = isDescriptionExpanded,
        ).withPrices(prices.state.value)
    }

    private fun showStaleMessage() {
        messageSender.send(SnackbarMessage(message = resourceReference(R.string.prediction_main_data_outdated)))
    }

    private companion object {
        val POLL_INTERVAL = 10.seconds
    }
}