package com.tangem.features.polymarket.impl.positiondetails.model

import arrow.core.getOrElse
import com.tangem.core.decompose.di.ModelScoped
import com.tangem.core.decompose.model.Model
import com.tangem.core.decompose.model.ParamsContainer
import com.tangem.core.decompose.navigation.Router
import com.tangem.core.decompose.ui.UiMessageSender
import com.tangem.core.navigation.url.UrlOpener
import com.tangem.domain.polymarket.PolymarketPositionsRepository
import com.tangem.domain.polymarket.model.PolymarketEvent
import com.tangem.domain.polymarket.model.PolymarketMarket
import com.tangem.domain.polymarket.model.PolymarketOutcome
import com.tangem.domain.polymarket.model.PolymarketPosition
import com.tangem.domain.polymarket.model.PolymarketResolutionStatus
import com.tangem.domain.polymarket.model.findMarket
import com.tangem.domain.polymarket.model.findOutcome
import com.tangem.domain.polymarket.usecase.DerivePolymarketAddressesUseCase
import com.tangem.domain.polymarket.usecase.GetPolymarketEventUseCase
import com.tangem.features.polymarket.impl.common.PolymarketConst
import com.tangem.features.polymarket.impl.common.region.PredictionRegionAccess
import com.tangem.features.polymarket.impl.common.region.regionRestrictionsMessage
import com.tangem.features.polymarket.impl.navigation.PolymarketRoute
import com.tangem.features.polymarket.impl.positiondetails.PolymarketPositionDetailsComponent
import com.tangem.features.polymarket.impl.positiondetails.ui.state.PolymarketPositionDetailsUM
import com.tangem.features.polymarket.impl.positiondetails.ui.state.PolymarketPositionDetailsUM.Content
import com.tangem.utils.coroutines.CoroutineDispatcherProvider
import com.tangem.utils.logging.TangemLogger
import kotlinx.coroutines.Job
import kotlinx.coroutines.delay
import kotlinx.coroutines.ensureActive
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import javax.inject.Inject

@ModelScoped
@Suppress("LongParameterList")
internal class PolymarketPositionDetailsModel @Inject constructor(
    paramsContainer: ParamsContainer,
    private val router: Router,
    override val dispatchers: CoroutineDispatcherProvider,
    private val repository: PolymarketPositionsRepository,
    private val deriveAddresses: DerivePolymarketAddressesUseCase,
    private val getEvent: GetPolymarketEventUseCase,
    private val messageSender: UiMessageSender,
    private val regionAccess: PredictionRegionAccess,
    private val urlOpener: UrlOpener,
) : Model() {
    private val params = paramsContainer.require<PolymarketPositionDetailsComponent.Params>()
    private val converter = PolymarketPositionDetailsConverter()
    private var isResumed = false
    private var loadJob: Job? = null
    private var eventJob: Job? = null
    private var position: PolymarketPosition.Open? = null
    private var hasShownPosition = false
    private var event: PolymarketEvent? = null
    private var isEventSettled = false

    val uiState: StateFlow<PolymarketPositionDetailsUM>
        field = MutableStateFlow(
            PolymarketPositionDetailsUM(
                content = Content.Loading,
                onCloseClick = { router.pop() },
                onMarketClosedSheetDismiss = ::hideMarketClosedSheet,
            ),
        )

    fun onResume() {
        if (isResumed) return
        isResumed = true
        reload()
    }

    fun onPause() {
        isResumed = false
        loadJob?.cancel()
        eventJob?.cancel()
    }

    private fun reload() {
        if (!isResumed) return
        loadJob?.cancel()
        eventJob?.cancel()
        val shownSize = position?.size
        position = null
        event = null
        isEventSettled = false
        uiState.update { it.copy(content = Content.Loading) }
        loadJob = modelScope.launch {
            val addresses = withContext(dispatchers.default) { deriveAddresses.stored(params.userWalletId) }
            ensureActive()
            if (addresses == null) {
                showError("Stored addresses unavailable")
                return@launch
            }
            for (read in 0 until PolymarketConst.POSITION_INDEXING_READS) {
                val result = withContext(dispatchers.default) { repository.getPosition(addresses, params.positionId) }
                ensureActive()
                val loaded = result.getOrElse { error ->
                    showError(error.toString())
                    return@launch
                }
                when {
                    loaded == null && !hasShownPosition -> {
                        showPosition(null)
                        return@launch
                    }
                    loaded != null && loaded.size != position?.size -> showPosition(loaded)
                }
                val isLastRead = read == PolymarketConst.POSITION_INDEXING_READS - 1
                val isStillIndexing = !isLastRead && (loaded == null || loaded.size == shownSize)
                if (!isStillIndexing) break
                delay(PolymarketConst.POSITION_INDEXING_RETRY_MILLIS)
            }
            if (position == null) showPosition(null)
        }
    }

    private fun showPosition(loaded: PolymarketPosition.Open?) {
        if (loaded == null && hasShownPosition) {
            router.pop()
            return
        }
        if (loaded == null) {
            uiState.update { it.copy(content = Content.Unavailable) }
            return
        }
        position = loaded
        hasShownPosition = true
        val isEventKnown = event?.id == loaded.eventId || eventJob?.isActive == true
        loaded.eventId?.takeUnless { isEventKnown }?.let(::loadEvent)
        publishPosition()
    }

    private fun loadEvent(eventId: String) {
        eventJob?.cancel()
        isEventSettled = false
        eventJob = modelScope.launch {
            val result = withContext(dispatchers.default) { getEvent(eventId) }
            ensureActive()
            if (position?.eventId != eventId) return@launch
            result.fold(
                ifLeft = { TangemLogger.e("Failed to load position event: $it") },
                ifRight = { loaded -> if (loaded.id == eventId) event = loaded },
            )
            isEventSettled = true
            publishPosition()
        }
    }

    private fun publishPosition() {
        val loaded = position ?: return
        val content = converter.convert(PolymarketPositionDetailsConverter.Input(position = loaded, event = event))
        uiState.update { state ->
            state.copy(
                content = content.copy(
                    event = content.event?.copy(onClick = loaded.eventId?.let { { openEvent() } }),
                    information = content.information.copy(onResolutionSourceClick = urlOpener::openUrl),
                    onClosePositionClick = closePositionAction(loaded),
                    onBuyMoreClick = buyMoreAction(loaded),
                ),
            )
        }
    }

    private fun openEvent() {
        val loaded = position ?: return
        val eventId = loaded.eventId ?: return
        val market = eventMarket(loaded)
        router.push(
            PolymarketRoute.EventDetails(
                eventId = eventId,
                userWalletId = params.userWalletId,
                marketId = loaded.marketId,
                assetId = market?.findOutcome(loaded)?.assetId ?: loaded.id.assetId,
            ),
        )
    }

    private fun closePositionAction(loaded: PolymarketPosition.Open): () -> Unit = when {
        loaded.canClosePosition && loaded.eventId != null && loaded.isMarketTradable -> ::closePosition
        else -> ::showMarketClosedSheet
    }

    /**
     * Whether orders are accepted is read off the event's market, not the position: a position still held in a
     * market's legacy token reports that it accepts no orders while the market itself trades on under its new
     * token. The resolution status stays the position's.
     */
    private fun buyMoreAction(loaded: PolymarketPosition.Open): () -> Unit {
        val market = eventMarket(loaded)
        val outcome = market?.findOutcome(loaded)
        val isEventLoading = loaded.eventId != null && !isEventSettled
        return when {
            loaded.marketId == null || loaded.resolutionStatus != PolymarketResolutionStatus.OPEN ->
                ::showMarketClosedSheet
            market == null && isEventLoading -> {
                {}
            }
            market == null || outcome == null -> ::showMarketClosedSheet
            market.isAcceptingOrders -> {
                { buyMore(market = market, outcome = outcome) }
            }
            else -> ::showMarketClosedSheet
        }
    }

    private fun showMarketClosedSheet() {
        uiState.update { it.copy(isMarketClosedSheetShown = true) }
    }

    private fun hideMarketClosedSheet() {
        uiState.update { it.copy(isMarketClosedSheetShown = false) }
    }

    private fun closePosition() {
        if (regionAccess.isRestricted) return messageSender.send(regionRestrictionsMessage())

        val loaded = position ?: return
        val marketId = loaded.marketId ?: return
        router.push(
            PolymarketRoute.ClosePosition(
                userWalletId = params.userWalletId,
                positionId = loaded.id,
                marketId = marketId,
            ),
        )
    }

    private fun eventMarket(loaded: PolymarketPosition.Open): PolymarketMarket? =
        event?.takeIf { it.id == loaded.eventId }?.findMarket(loaded)

    private fun buyMore(market: PolymarketMarket, outcome: PolymarketOutcome) {
        if (regionAccess.isRestricted) return messageSender.send(regionRestrictionsMessage())

        val loaded = position ?: return
        val eventId = loaded.eventId ?: return
        router.push(
            PolymarketRoute.PlacePrediction(
                userWalletId = params.userWalletId,
                eventId = eventId,
                marketId = market.id,
                assetId = outcome.assetId,
            ),
        )
    }

    private fun showError(reason: String) {
        TangemLogger.e("Failed to load Polymarket position: $reason")
        uiState.update { it.copy(content = Content.Error(::reload)) }
    }
}