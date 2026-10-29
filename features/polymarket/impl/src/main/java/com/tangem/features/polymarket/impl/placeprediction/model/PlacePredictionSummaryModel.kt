package com.tangem.features.polymarket.impl.placeprediction.model

import com.arkivanov.decompose.router.slot.SlotNavigation
import com.arkivanov.decompose.router.slot.activate
import com.arkivanov.decompose.router.slot.dismiss
import com.tangem.core.decompose.di.ModelScoped
import com.tangem.core.decompose.model.Model
import com.tangem.core.decompose.model.ParamsContainer
import com.tangem.core.decompose.navigation.Router
import com.tangem.core.navigation.url.UrlOpener
import com.tangem.features.polymarket.impl.common.PolymarketLegalUrls
import com.tangem.features.polymarket.impl.placeprediction.PlacePredictionRoute
import com.tangem.features.polymarket.impl.placeprediction.entity.PlacePredictionUM
import com.tangem.features.polymarket.impl.placeprediction.entity.PredictionFeeSheetUM
import com.tangem.features.polymarket.impl.placeprediction.entity.PreparedPrediction
import com.tangem.features.polymarket.impl.placeprediction.entity.canShowFeeBreakdown
import com.tangem.features.polymarket.impl.placeprediction.entity.feeBreakdownQuote
import com.tangem.features.polymarket.impl.placeprediction.entity.toStatusRoute
import com.tangem.features.polymarket.impl.placeprediction.model.converter.PredictionFeeSheetUMConverter
import com.tangem.utils.coroutines.CoroutineDispatcherProvider
import kotlinx.coroutines.CoroutineStart
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.filter
import kotlinx.coroutines.flow.launchIn
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.onEach
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch
import javax.inject.Inject

@ModelScoped
internal class PlacePredictionSummaryModel @Inject constructor(
    paramsContainer: ParamsContainer,
    private val router: Router,
    private val urlOpener: UrlOpener,
    override val dispatchers: CoroutineDispatcherProvider,
    orderSubmitterFactory: PlacePredictionOrderSubmitter.Factory,
) : Model(), PlacePredictionSummaryIntents {

    private val order = paramsContainer.require<PreparedPrediction>()
    private val params = order.params

    val uiState: StateFlow<PlacePredictionUM>
        field = MutableStateFlow(order.state)

    private val orderSubmitter = orderSubmitterFactory.create(params = params, uiState = uiState, scope = modelScope)

    val feeSheetNavigation = SlotNavigation<Unit>()

    val feeSheetState: StateFlow<PredictionFeeSheetUM?> = uiState
        .map(::createFeeSheet)
        .stateIn(scope = modelScope, started = SharingStarted.Eagerly, initialValue = createFeeSheet(order.state))

    init {
        dismissFeeSheetWhenUnavailable()
    }

    override fun onFeeClick() {
        if (uiState.value.canShowFeeBreakdown()) feeSheetNavigation.activate(Unit)
    }

    private fun createFeeSheet(state: PlacePredictionUM): PredictionFeeSheetUM? {
        val content = state.feeBreakdownQuote() ?: return null

        return PredictionFeeSheetUMConverter.convert(
            quote = content.signable.quote,
            slippage = state.slippage,
            tokenSymbol = state.collateral.tokenSymbol,
        )
    }

    private fun dismissFeeSheetWhenUnavailable() {
        feeSheetState
            .filter { it == null }
            .onEach { feeSheetNavigation.dismiss() }
            .launchIn(modelScope)
    }

    override fun onPlaceClick() {
        modelScope.launch(start = CoroutineStart.UNDISPATCHED) {
            val result = orderSubmitter.submit(prepared = order) ?: return@launch
            router.replaceAll(order.toStatusRoute(result = result))
        }
    }

    override fun onQuoteRetryClick() {
        if (!isSubmitting()) router.popTo(PlacePredictionRoute.Amount)
    }

    override fun onPolymarketTermsClick() {
        urlOpener.openUrl(PolymarketLegalUrls.polymarketTerms)
    }

    override fun onTangemTermsClick() {
        urlOpener.openUrl(PolymarketLegalUrls.TANGEM_TERMS)
    }

    override fun onBackClick() {
        if (isSubmitting()) return

        router.pop()
    }

    override fun onCloseClick() {
        if (isSubmitting()) return

        router.popTo(PlacePredictionRoute.Amount)
        router.pop()
    }

    fun isSubmitting(): Boolean = orderSubmitter.isSubmitting()
}