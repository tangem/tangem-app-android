package com.tangem.features.polymarket.impl.details.model

import arrow.core.Either
import com.tangem.core.decompose.di.ModelScoped
import com.tangem.core.decompose.model.Model
import com.tangem.core.decompose.model.ParamsContainer
import com.tangem.core.decompose.navigation.Router
import com.tangem.core.decompose.ui.UiMessageSender
import com.tangem.core.navigation.share.ShareManager
import com.tangem.core.navigation.url.UrlOpener
import com.tangem.core.ui.extensions.resourceReference
import com.tangem.core.ui.message.SnackbarMessage
import com.tangem.domain.polymarket.model.PolymarketEvent
import com.tangem.domain.polymarket.model.PolymarketEventError
import com.tangem.domain.polymarket.usecase.GetPolymarketEventUseCase
import com.tangem.features.polymarket.impl.R
import com.tangem.features.polymarket.impl.common.PolymarketUrlBuilder
import com.tangem.features.polymarket.impl.common.region.PredictionRegionAccess
import com.tangem.features.polymarket.impl.common.region.regionRestrictionsMessage
import com.tangem.features.polymarket.impl.details.PolymarketEventDetailsComponent
import com.tangem.features.polymarket.impl.details.model.transformer.PolymarketEventDetailsContentTransformer
import com.tangem.features.polymarket.impl.details.ui.state.PolymarketEventDetailsUM
import com.tangem.features.polymarket.impl.navigation.PolymarketRoute
import com.tangem.utils.coroutines.CoroutineDispatcherProvider
import com.tangem.utils.coroutines.JobHolder
import com.tangem.utils.coroutines.saveIn
import com.tangem.utils.logging.TangemLogger
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import javax.inject.Inject
import kotlin.time.Duration.Companion.seconds

/**
 * Model of the event-details screen: loads the event with all of its markets, keeps it fresh while the screen
 * is in front of the user and drives the closed-markets / description folds.
 *
 * The shown event is re-requested every [POLL_INTERVAL] and at once when the screen comes back (from the
 * order flow or from the background). A refresh that fails keeps the last event on screen and reports it with
 * a snackbar once, until a refresh succeeds again.
 */
@ModelScoped
@Suppress("LongParameterList")
internal class PolymarketEventDetailsModel @Inject constructor(
    paramsContainer: ParamsContainer,
    private val router: Router,
    override val dispatchers: CoroutineDispatcherProvider,
    private val getPolymarketEventUseCase: GetPolymarketEventUseCase,
    private val shareManager: ShareManager,
    private val messageSender: UiMessageSender,
    private val regionAccess: PredictionRegionAccess,
    private val urlOpener: UrlOpener,
) : Model() {

    private val params = paramsContainer.require<PolymarketEventDetailsComponent.Params>()

    val uiState: StateFlow<PolymarketEventDetailsUM>
        field = MutableStateFlow<PolymarketEventDetailsUM>(PolymarketEventDetailsUM.Loading)

    private var event: PolymarketEvent? = null
    private var isClosedMarketsExpanded = false
    private var isDescriptionExpanded = false
    private var isResumed = false
    private var isStaleDataReported = false

    private val loadJob = JobHolder()
    private val pollJob = JobHolder()

    init {
        load()
    }

    fun onBackClick() {
        router.pop()
    }

    fun onResume() {
        isResumed = true
        if (uiState.value is PolymarketEventDetailsUM.Content) startPolling(refreshNow = true)
    }

    fun onPause() {
        isResumed = false
        pollJob.cancel()
    }

    private fun load() {
        pollJob.cancel()
        modelScope.launch {
            uiState.value = PolymarketEventDetailsUM.Loading
            val result = withContext(dispatchers.default) { fetchEventWithSilentRetry() }
            result.fold(
                ifLeft = { uiState.value = PolymarketEventDetailsUM.Error(onRetryClick = ::load) },
                ifRight = { loaded ->
                    showEvent(loaded)
                    if (isResumed) startPolling(refreshNow = false)
                },
            )
        }.saveIn(loadJob)
    }

    private fun startPolling(refreshNow: Boolean) {
        modelScope.launch {
            if (refreshNow) refresh()
            while (true) {
                delay(POLL_INTERVAL)
                refresh()
            }
        }.saveIn(pollJob)
    }

    private suspend fun refresh() {
        val result = withContext(dispatchers.default) { fetchEventWithSilentRetry() }
        result.fold(
            ifLeft = { error ->
                TangemLogger.e("Event details: refresh of ${params.eventId} failed, keeping the last data: $error")
                reportStaleDataOnce()
            },
            ifRight = { loaded ->
                isStaleDataReported = false
                showEvent(loaded)
            },
        )
    }

    private suspend fun fetchEventWithSilentRetry(): Either<PolymarketEventError, PolymarketEvent> {
        val first = getPolymarketEventUseCase(eventId = params.eventId)
        if (first.isRight()) return first

        delay(RETRY_DELAY)
        return getPolymarketEventUseCase(eventId = params.eventId)
    }

    private fun reportStaleDataOnce() {
        if (isStaleDataReported) return
        isStaleDataReported = true
        messageSender.send(SnackbarMessage(message = resourceReference(R.string.prediction_main_data_outdated)))
    }

    private fun showEvent(loaded: PolymarketEvent) {
        event = loaded
        refreshContent()
    }

    private fun contentTransformer(event: PolymarketEvent): PolymarketEventDetailsContentTransformer {
        return PolymarketEventDetailsContentTransformer(
            event = event,
            categoryLabel = params.categoryLabel,
            isClosedMarketsExpanded = isClosedMarketsExpanded,
            isDescriptionExpanded = isDescriptionExpanded,
            onShareClick = ::onShareClick,
            onOutcomeClick = ::onOutcomeClick,
            onClosedMarketsClick = ::onClosedMarketsClick,
            onReadMoreClick = ::onReadMoreClick,
            onResolutionSourceClick = urlOpener::openUrl,
        )
    }

    private fun onShareClick(slug: String) {
        val url = PolymarketUrlBuilder.build(page = PolymarketUrlBuilder.Page.Event(slug = slug))
        shareManager.shareText(text = url)
    }

    private fun onOutcomeClick(marketId: String, assetId: String) {
        if (regionAccess.isRestricted) return messageSender.send(regionRestrictionsMessage())

        router.push(
            PolymarketRoute.PlacePrediction(
                userWalletId = params.userWalletId,
                eventId = params.eventId,
                marketId = marketId,
                assetId = assetId,
            ),
        )
    }

    private fun onClosedMarketsClick() {
        isClosedMarketsExpanded = !isClosedMarketsExpanded
        refreshContent()
    }

    private fun onReadMoreClick() {
        isDescriptionExpanded = true
        refreshContent()
    }

    private fun refreshContent() {
        val loaded = event ?: return
        uiState.value = contentTransformer(event = loaded).transform(prevState = uiState.value)
    }

    private companion object {
        val POLL_INTERVAL = 10.seconds
        val RETRY_DELAY = 2.seconds
    }
}