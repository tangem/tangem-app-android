package com.tangem.features.polymarket.impl.placeprediction.model

import com.arkivanov.decompose.router.slot.SlotNavigation
import com.tangem.core.decompose.di.ModelScoped
import com.tangem.core.decompose.model.Model
import com.tangem.core.decompose.model.ParamsContainer
import com.tangem.core.decompose.navigation.Router
import com.tangem.core.navigation.url.UrlOpener
import com.tangem.features.polymarket.impl.common.PolymarketLegalUrls
import com.tangem.features.polymarket.impl.common.formatPolymarketEnteredAmount
import com.tangem.features.polymarket.impl.main.addfunds.PolymarketAddFundsComponent
import com.tangem.features.polymarket.impl.main.addfunds.PolymarketFundingSheetConfig
import com.tangem.features.polymarket.impl.placeprediction.PlacePredictionComponent
import com.tangem.features.polymarket.impl.placeprediction.PlacePredictionRoute
import com.tangem.features.polymarket.impl.placeprediction.entity.AmountPresetUM
import com.tangem.features.polymarket.impl.placeprediction.entity.OrderSideUM
import com.tangem.features.polymarket.impl.placeprediction.entity.PlacePredictionUM
import com.tangem.features.polymarket.impl.placeprediction.entity.QuoteUM
import com.tangem.features.polymarket.impl.placeprediction.entity.SellPositionUM
import com.tangem.features.polymarket.impl.placeprediction.entity.toStatusRoute
import com.tangem.utils.coroutines.CoroutineDispatcherProvider
import kotlinx.collections.immutable.ImmutableList
import kotlinx.collections.immutable.persistentListOf
import kotlinx.collections.immutable.toImmutableList
import kotlinx.coroutines.CoroutineStart
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.launch
import java.math.BigDecimal
import java.math.MathContext
import java.math.RoundingMode
import javax.inject.Inject

@Suppress("TooManyFunctions")
@ModelScoped
internal class PlacePredictionOrderModel @Inject constructor(
    paramsContainer: ParamsContainer,
    private val router: Router,
    private val urlOpener: UrlOpener,
    override val dispatchers: CoroutineDispatcherProvider,
    quoteEngineFactory: PlacePredictionQuoteEngine.Factory,
    orderSubmitterFactory: PlacePredictionOrderSubmitter.Factory,
) : Model(), PlacePredictionOrderIntents, PolymarketAddFundsComponent.ModelCallback {

    private val params = paramsContainer.require<PlacePredictionComponent.Params>()

    val uiState: StateFlow<PlacePredictionUM>
        field = MutableStateFlow(PlacePredictionUM.initial(side = params.side))

    private val quoteEngine = quoteEngineFactory.create(params = params, uiState = uiState, scope = modelScope)

    private val orderSubmitter = orderSubmitterFactory.create(params = params, uiState = uiState, scope = modelScope)

    val feeSheet = PlacePredictionFeeSheet(uiState = uiState, scope = modelScope)

    val fundingSheetNavigation: SlotNavigation<PolymarketFundingSheetConfig>
        get() = quoteEngine.addFunds.navigation

    val amountPresets: ImmutableList<AmountPresetUM> = when (params) {
        is PlacePredictionComponent.Params.Buy -> BUY_PRESET_AMOUNTS.map(::toPreset).toImmutableList()
        is PlacePredictionComponent.Params.Sell -> persistentListOf()
    }

    fun onResume() {
        quoteEngine.onResume()
    }

    fun onPause() {
        quoteEngine.onPause()
    }

    fun isSubmitting(): Boolean = orderSubmitter.isSubmitting()

    override fun onAmountChange(value: String) {
        if (isSubmitting() || value == uiState.value.amountValue) return

        quoteEngine.onAmountChange(value = value)
    }

    override fun onPresetClick(preset: AmountPresetUM) {
        onAmountChange(value = preset.amount)
    }

    override fun onMaxClick() {
        val max = when (val order = uiState.value.order) {
            is OrderSideUM.Buy -> maxBuyAmount(balance = order.balance ?: return)
            is OrderSideUM.Sell -> (order.position as? SellPositionUM.Held)?.available
        } ?: return

        onAmountChange(value = max.toPlainString())
    }

    override fun onSlippageClick() {
        if (isSubmitting()) return

        router.push(
            PlacePredictionRoute.Slippage(
                value = uiState.value.slippage.percent,
                onConfirm = quoteEngine::applySlippage,
            ),
        )
    }

    override fun onFeeClick() {
        feeSheet.onFeeClick()
    }

    override fun onAddFundsClick() {
        if (isSubmitting()) return

        quoteEngine.addFunds.onAddFundsClick()
    }

    override fun onSwapClick() {
        if (isSubmitting()) return

        quoteEngine.addFunds.onSwapClick()
    }

    override fun onReceiveClick() {
        if (isSubmitting()) return

        quoteEngine.addFunds.onReceiveClick()
    }

    override fun onDismissAddFunds() {
        quoteEngine.addFunds.onDismissAddFunds()
    }

    override fun onQuoteRetryClick() {
        quoteEngine.retryQuote()
    }

    override fun onPlaceClick() {
        val prepared = quoteEngine.prepare() ?: return

        modelScope.launch(start = CoroutineStart.UNDISPATCHED) {
            quoteEngine.holdDuring {
                val result = orderSubmitter.submit(prepared = prepared) ?: return@holdDuring
                router.replaceAll(prepared.toStatusRoute(result = result))
            }
        }
    }

    override fun onPolymarketTermsClick() {
        urlOpener.openUrl(PolymarketLegalUrls.polymarketTerms)
    }

    override fun onTangemTermsClick() {
        urlOpener.openUrl(PolymarketLegalUrls.TANGEM_TERMS)
    }

    override fun onBackClick() {
        leaveFlow()
    }

    override fun onCloseClick() {
        leaveFlow()
    }

    private fun leaveFlow() {
        if (isSubmitting()) return

        router.pop()
    }

    private fun maxBuyAmount(balance: BigDecimal): BigDecimal? {
        val balanceCap = balance.multiply(MAX_BALANCE_SHARE)
        val feeCap = latestFeeRate()?.let { feeRate ->
            balance.divide(BigDecimal.ONE.add(feeRate), MathContext.DECIMAL128)
        }
        val max = (feeCap?.min(balanceCap) ?: balanceCap).setScale(AMOUNT_DECIMALS, RoundingMode.FLOOR)

        return max.takeIf { it >= MIN_MAX_AMOUNT }?.stripTrailingZeros()
    }

    private fun latestFeeRate(): BigDecimal? {
        val quote = uiState.value.quote as? QuoteUM.Content ?: return null
        val notional = quote.signable.quote.notional
        if (notional.signum() <= 0) return null

        return quote.feeTotal.divide(notional, MathContext.DECIMAL128)
    }

    private fun toPreset(amount: BigDecimal): AmountPresetUM = AmountPresetUM(
        label = amount.formatPolymarketEnteredAmount(),
        amount = amount.toPlainString(),
    )

    private companion object {
        val BUY_PRESET_AMOUNTS = listOf(BigDecimal(100), BigDecimal(500), BigDecimal(1_000), BigDecimal(5_000))
        val MAX_BALANCE_SHARE = BigDecimal("0.9")
        val MIN_MAX_AMOUNT = BigDecimal("0.01")
        const val AMOUNT_DECIMALS = 2
    }
}