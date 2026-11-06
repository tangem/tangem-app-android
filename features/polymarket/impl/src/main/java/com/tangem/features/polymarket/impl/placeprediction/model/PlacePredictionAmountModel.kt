package com.tangem.features.polymarket.impl.placeprediction.model

import com.arkivanov.decompose.router.slot.SlotNavigation
import com.tangem.core.decompose.di.ModelScoped
import com.tangem.core.decompose.model.Model
import com.tangem.core.decompose.model.ParamsContainer
import com.tangem.core.decompose.navigation.Router
import com.tangem.features.polymarket.impl.main.addfunds.PolymarketAddFundsComponent
import com.tangem.features.polymarket.impl.main.addfunds.PolymarketFundingSheetConfig
import com.tangem.features.polymarket.impl.placeprediction.PlacePredictionComponent
import com.tangem.features.polymarket.impl.placeprediction.PlacePredictionRoute
import com.tangem.features.polymarket.impl.placeprediction.entity.AmountFractionUM
import com.tangem.features.polymarket.impl.placeprediction.entity.PlacePredictionUM
import com.tangem.utils.coroutines.CoroutineDispatcherProvider
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import javax.inject.Inject

@ModelScoped
internal class PlacePredictionAmountModel @Inject constructor(
    paramsContainer: ParamsContainer,
    private val router: Router,
    override val dispatchers: CoroutineDispatcherProvider,
    quoteEngineFactory: PlacePredictionQuoteEngine.Factory,
) : Model(), PlacePredictionAmountIntents, PolymarketAddFundsComponent.ModelCallback {

    private val params = paramsContainer.require<PlacePredictionComponent.Params>()

    val uiState: StateFlow<PlacePredictionUM>
        field = MutableStateFlow(PlacePredictionUM.initial(side = params.side))

    private val quoteEngine = quoteEngineFactory.create(params = params, uiState = uiState, scope = modelScope)

    val fundingSheetNavigation: SlotNavigation<PolymarketFundingSheetConfig>
        get() = quoteEngine.addFunds.navigation

    fun onResume() {
        quoteEngine.onResume()
    }

    fun onPause() {
        quoteEngine.onPause()
    }

    override fun onAmountChange(value: String) {
        quoteEngine.onAmountChange(value = value)
    }

    override fun onFractionClick(fraction: AmountFractionUM) {
        quoteEngine.onFractionClick(fraction = fraction)
    }

    override fun onSlippageClick() {
        router.push(
            PlacePredictionRoute.Slippage(
                value = uiState.value.slippage.percent,
                onConfirm = quoteEngine::applySlippage,
            ),
        )
    }

    override fun onAddFundsClick() {
        quoteEngine.addFunds.onAddFundsClick()
    }

    override fun onSwapClick() {
        quoteEngine.addFunds.onSwapClick()
    }

    override fun onReceiveClick() {
        quoteEngine.addFunds.onReceiveClick()
    }

    override fun onDismissAddFunds() {
        quoteEngine.addFunds.onDismissAddFunds()
    }

    override fun onQuoteRetryClick() {
        quoteEngine.retryQuote()
    }

    override fun onNextClick() {
        val order = quoteEngine.prepare() ?: return
        router.push(PlacePredictionRoute.Summary(order = order))
    }

    override fun onBackClick() {
        router.pop()
    }

    override fun onCloseClick() {
        router.popTo(PlacePredictionRoute.Amount)
        router.pop()
    }
}