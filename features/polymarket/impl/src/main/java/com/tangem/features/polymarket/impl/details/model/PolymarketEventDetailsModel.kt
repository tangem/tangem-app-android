package com.tangem.features.polymarket.impl.details.model

import com.tangem.core.decompose.di.ModelScoped
import com.tangem.core.decompose.model.Model
import com.tangem.core.decompose.model.ParamsContainer
import com.tangem.core.decompose.navigation.Router
import com.tangem.core.navigation.share.ShareManager
import com.tangem.domain.polymarket.model.PolymarketEvent
import com.tangem.domain.polymarket.model.PredictionOrderSide
import com.tangem.domain.polymarket.usecase.GetPolymarketEventUseCase
import com.tangem.features.polymarket.impl.common.PolymarketUrlBuilder
import com.tangem.features.polymarket.impl.details.PolymarketEventDetailsComponent
import com.tangem.features.polymarket.impl.details.model.transformer.PolymarketEventDetailsContentTransformer
import com.tangem.features.polymarket.impl.details.ui.state.PolymarketEventDetailsUM
import com.tangem.features.polymarket.impl.navigation.PolymarketRoute
import com.tangem.utils.coroutines.CoroutineDispatcherProvider
import com.tangem.utils.coroutines.JobHolder
import com.tangem.utils.coroutines.saveIn
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import javax.inject.Inject

/**
 * Model of the event-details screen: loads the event with all of its markets and drives the
 * closed-markets / description folds.
 */
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

    private var event: PolymarketEvent? = null
    private var isClosedMarketsExpanded = false
    private var isDescriptionExpanded = false

    private val loadJob = JobHolder()

    init {
        load()
    }

    fun onBackClick() {
        router.pop()
    }

    private fun load() {
        modelScope.launch {
            uiState.value = PolymarketEventDetailsUM.Loading
            val newState = withContext(dispatchers.default) {
                getPolymarketEventUseCase(eventId = params.eventId).fold(
                    ifLeft = { PolymarketEventDetailsUM.Error(onRetryClick = ::load) },
                    ifRight = { loaded ->
                        event = loaded
                        contentTransformer(event = loaded).transform(prevState = uiState.value)
                    },
                )
            }
            uiState.value = newState
        }.saveIn(loadJob)
    }

    private fun contentTransformer(event: PolymarketEvent): PolymarketEventDetailsContentTransformer {
        return PolymarketEventDetailsContentTransformer(
            event = event,
            isClosedMarketsExpanded = isClosedMarketsExpanded,
            isDescriptionExpanded = isDescriptionExpanded,
            onShareClick = ::onShareClick,
            onOutcomeClick = ::onOutcomeClick,
            onClosedMarketsClick = ::onClosedMarketsClick,
            onReadMoreClick = ::onReadMoreClick,
        )
    }

    private fun onShareClick(slug: String) {
        val url = PolymarketUrlBuilder.build(page = PolymarketUrlBuilder.Page.Event(slug = slug))
        shareManager.shareText(text = url)
    }

    private fun onOutcomeClick(marketId: String, assetId: String) {
        router.push(
            PolymarketRoute.PlacePrediction(
                userWalletId = params.userWalletId,
                eventId = params.eventId,
                marketId = marketId,
                assetId = assetId,
                side = PredictionOrderSide.BUY,
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
}