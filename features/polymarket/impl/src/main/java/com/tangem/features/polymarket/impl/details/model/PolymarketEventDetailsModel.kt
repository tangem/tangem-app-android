package com.tangem.features.polymarket.impl.details.model

import com.tangem.core.decompose.di.ModelScoped
import com.tangem.core.decompose.model.Model
import com.tangem.core.decompose.model.ParamsContainer
import com.tangem.core.decompose.navigation.Router
import com.tangem.core.navigation.share.ShareManager
import com.tangem.domain.polymarket.model.PolymarketEvent
import com.tangem.domain.polymarket.usecase.GetPolymarketEventUseCase
import com.tangem.features.polymarket.impl.common.PolymarketUrlBuilder
import com.tangem.features.polymarket.impl.details.PolymarketEventDetailsComponent
import com.tangem.features.polymarket.impl.details.model.transformer.PolymarketEventDetailsContentTransformer
import com.tangem.features.polymarket.impl.details.ui.state.PolymarketEventDetailsUM
import com.tangem.features.polymarket.impl.placeprediction.PlacePredictionConfig
import com.tangem.utils.coroutines.CoroutineDispatcherProvider
import com.tangem.utils.coroutines.JobHolder
import com.tangem.utils.coroutines.saveIn
import kotlinx.coroutines.channels.BufferOverflow
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharedFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import javax.inject.Inject

/** Model of the event-details sheet: loads the event with all of its markets. */
@ModelScoped
internal class PolymarketEventDetailsModel @Inject constructor(
    paramsContainer: ParamsContainer,
    private val router: Router,
    override val dispatchers: CoroutineDispatcherProvider,
    private val getPolymarketEventUseCase: GetPolymarketEventUseCase,
    private val shareManager: ShareManager,
) : Model() {

    private val params = paramsContainer.require<PolymarketEventDetailsComponent.Params>()

    val uiState: StateFlow<PolymarketEventDetailsUM>
        field = MutableStateFlow<PolymarketEventDetailsUM>(PolymarketEventDetailsUM.Loading)

    /** Requests to open the Place-prediction sheet; the component activates its child slot on each one. */
    val sheetRequests: SharedFlow<PlacePredictionConfig>
        field = MutableSharedFlow<PlacePredictionConfig>(
            extraBufferCapacity = 1,
            onBufferOverflow = BufferOverflow.DROP_OLDEST,
        )

    private val loadJob = JobHolder()

    init {
        load()
    }

    fun onCloseClick() {
        router.pop()
    }

    private fun load() {
        modelScope.launch {
            uiState.value = PolymarketEventDetailsUM.Loading
            val newState = withContext(dispatchers.default) {
                getPolymarketEventUseCase(eventId = params.eventId).fold(
                    ifLeft = { PolymarketEventDetailsUM.Error(onRetryClick = ::load) },
                    ifRight = { loaded -> contentTransformer(event = loaded).transform(prevState = uiState.value) },
                )
            }
            uiState.value = newState
        }.saveIn(loadJob)
    }

    private fun contentTransformer(event: PolymarketEvent): PolymarketEventDetailsContentTransformer {
        return PolymarketEventDetailsContentTransformer(
            event = event,
            onShareClick = ::onShareClick,
            onOutcomeClick = ::onOutcomeClick,
        )
    }

    private fun onShareClick(slug: String) {
        val url = PolymarketUrlBuilder.build(page = PolymarketUrlBuilder.Page.Event(slug = slug))
        shareManager.shareText(text = url)
    }

    private fun onOutcomeClick(marketId: String, assetId: String) {
        sheetRequests.tryEmit(
            PlacePredictionConfig(eventId = params.eventId, marketId = marketId, side = assetId),
        )
    }
}