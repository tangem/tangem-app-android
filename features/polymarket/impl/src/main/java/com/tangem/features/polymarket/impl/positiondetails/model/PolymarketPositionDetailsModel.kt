package com.tangem.features.polymarket.impl.positiondetails.model

import com.tangem.core.decompose.di.ModelScoped
import com.tangem.core.decompose.model.Model
import com.tangem.core.decompose.model.ParamsContainer
import com.tangem.core.decompose.navigation.Router
import com.tangem.domain.polymarket.PolymarketPositionsRepository
import com.tangem.domain.polymarket.model.PolymarketEvent
import com.tangem.domain.polymarket.model.PolymarketPosition
import com.tangem.domain.polymarket.usecase.DerivePolymarketAddressesUseCase
import com.tangem.domain.polymarket.usecase.GetPolymarketEventUseCase
import com.tangem.features.polymarket.impl.navigation.PolymarketRoute
import com.tangem.features.polymarket.impl.positiondetails.PolymarketPositionDetailsComponent
import com.tangem.features.polymarket.impl.positiondetails.ui.state.PolymarketPositionDetailsUM
import com.tangem.features.polymarket.impl.positiondetails.ui.state.PolymarketPositionDetailsUM.Content
import com.tangem.utils.coroutines.CoroutineDispatcherProvider
import com.tangem.utils.logging.TangemLogger
import kotlinx.coroutines.Job
import kotlinx.coroutines.ensureActive
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import javax.inject.Inject

@ModelScoped
internal class PolymarketPositionDetailsModel @Inject constructor(
    paramsContainer: ParamsContainer,
    private val router: Router,
    override val dispatchers: CoroutineDispatcherProvider,
    private val repository: PolymarketPositionsRepository,
    private val deriveAddresses: DerivePolymarketAddressesUseCase,
    private val getEvent: GetPolymarketEventUseCase,
) : Model() {
    private val params = paramsContainer.require<PolymarketPositionDetailsComponent.Params>()
    private val converter = PolymarketPositionDetailsConverter()
    private var isResumed = false
    private var loadJob: Job? = null
    private var eventJob: Job? = null
    private var position: PolymarketPosition.Open? = null
    private var event: PolymarketEvent? = null

    val uiState: StateFlow<PolymarketPositionDetailsUM>
        field = MutableStateFlow(PolymarketPositionDetailsUM(Content.Loading, onCloseClick = { router.pop() }))

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
        position = null
        event = null
        uiState.update { it.copy(content = Content.Loading) }
        loadJob = modelScope.launch {
            val result = withContext(dispatchers.default) {
                val addresses = deriveAddresses.stored(params.userWalletId) ?: return@withContext null
                repository.getPosition(addresses, params.positionId)
            }
            ensureActive()
            if (result == null) {
                showError("Stored addresses unavailable")
            } else {
                result.fold(ifLeft = { showError(it.toString()) }, ifRight = ::showPosition)
            }
        }
    }

    private fun showPosition(loaded: PolymarketPosition.Open?) {
        if (loaded == null || !loaded.canOpenPositionDetails) {
            uiState.update { it.copy(content = Content.Unavailable) }
            return
        }
        position = loaded
        publishPosition()
        loaded.eventId?.let(::loadEvent)
    }

    private fun loadEvent(eventId: String) {
        eventJob = modelScope.launch {
            val result = withContext(dispatchers.default) { getEvent(eventId) }
            ensureActive()
            result.fold(
                ifLeft = { TangemLogger.e("Failed to load position event: $it") },
                ifRight = { loaded ->
                    if (position?.eventId == eventId && loaded.id == eventId) {
                        event = loaded
                        publishPosition()
                    }
                },
            )
        }
    }

    private fun publishPosition() {
        val loaded = position ?: return
        val content = converter.convert(PolymarketPositionDetailsConverter.Input(position = loaded, event = event))
        uiState.update { state ->
            state.copy(
                content = content.copy(
                    event = content.event?.copy(onClick = loaded.eventId?.let { { openEvent() } }),
                ),
            )
        }
    }

    private fun openEvent() {
        val loaded = position ?: return
        val eventId = loaded.eventId ?: return
        router.push(
            PolymarketRoute.EventDetails(
                eventId = eventId,
                userWalletId = params.userWalletId,
                marketId = loaded.marketId,
                assetId = loaded.id.assetId,
            ),
        )
    }

    private fun showError(reason: String) {
        TangemLogger.e("Failed to load Polymarket position: $reason")
        uiState.update { it.copy(content = Content.Error(::reload)) }
    }
}