package com.tangem.features.polymarket.impl.placeprediction.model

import com.arkivanov.decompose.router.slot.SlotNavigation
import com.tangem.core.decompose.di.ModelScoped
import com.tangem.core.decompose.model.Model
import com.tangem.core.decompose.model.ParamsContainer
import com.tangem.core.decompose.navigation.Router
import com.tangem.core.navigation.url.UrlOpener
import com.tangem.core.ui.utils.parseBigDecimal
import com.tangem.core.ui.utils.parseBigDecimalOrNull
import com.tangem.features.polymarket.impl.common.PolymarketConst
import com.tangem.features.polymarket.impl.common.PolymarketLegalUrls
import com.tangem.features.polymarket.impl.common.formatPolymarketEnteredAmount
import com.tangem.features.polymarket.impl.di.PlacePredictionTimeSource
import com.tangem.features.polymarket.impl.main.addfunds.PolymarketAddFundsComponent
import com.tangem.features.polymarket.impl.main.addfunds.PolymarketFundingSheetConfig
import com.tangem.features.polymarket.impl.placeprediction.PlacePredictionComponent
import com.tangem.features.polymarket.impl.placeprediction.PlacePredictionRoute
import com.tangem.features.polymarket.impl.placeprediction.entity.AmountFractionUM
import com.tangem.features.polymarket.impl.placeprediction.entity.AmountPresetUM
import com.tangem.features.polymarket.impl.placeprediction.entity.MIN_QUOTABLE_AMOUNT
import com.tangem.features.polymarket.impl.placeprediction.entity.OrderSideUM
import com.tangem.features.polymarket.impl.placeprediction.entity.PlacePredictionUM
import com.tangem.features.polymarket.impl.placeprediction.entity.PreparedPrediction
import com.tangem.features.polymarket.impl.placeprediction.entity.PredictionFeeSheetUM
import com.tangem.features.polymarket.impl.placeprediction.entity.toStatusRoute
import com.tangem.utils.coroutines.CoroutineDispatcherProvider
import com.tangem.utils.coroutines.JobHolder
import com.tangem.utils.coroutines.saveIn
import kotlinx.collections.immutable.ImmutableList
import kotlinx.collections.immutable.persistentListOf
import kotlinx.collections.immutable.toImmutableList
import kotlinx.coroutines.CoroutineStart
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import java.math.BigDecimal
import java.math.RoundingMode
import javax.inject.Inject
import kotlin.time.Duration.Companion.seconds
import kotlin.time.TimeSource

@Suppress("LongParameterList", "TooManyFunctions")
@ModelScoped
internal class PlacePredictionOrderModel @Inject constructor(
    paramsContainer: ParamsContainer,
    private val router: Router,
    private val urlOpener: UrlOpener,
    override val dispatchers: CoroutineDispatcherProvider,
    quoteEngineFactory: PlacePredictionQuoteEngine.Factory,
    orderSubmitterFactory: PlacePredictionOrderSubmitter.Factory,
    @PlacePredictionTimeSource private val timeSource: TimeSource,
) : Model(), PlacePredictionOrderIntents, PolymarketAddFundsComponent.ModelCallback {

    private val params = paramsContainer.require<PlacePredictionComponent.Params>()

    val uiState: StateFlow<PlacePredictionUM>
        field = MutableStateFlow(PlacePredictionUM.initial(side = params.side))

    private val quoteEngine = quoteEngineFactory.create(params = params, uiState = uiState, scope = modelScope)

    private val orderSubmitter = orderSubmitterFactory.create(params = params, uiState = uiState, scope = modelScope)

    private val pendingPlacementJobHolder = JobHolder()

    val feeSheet = PlacePredictionFeeSheet(
        uiState = uiState,
        scope = modelScope,
        heading = PredictionFeeSheetUM.Heading.Total,
    )

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
        dropPendingPlacement()
        quoteEngine.onPause()
    }

    fun isSubmitting(): Boolean = orderSubmitter.isSubmitting() || uiState.value.isPlacementPending

    override fun onAmountChange(value: String) {
        if (orderSubmitter.isSubmitting() || value == uiState.value.amountValue) return

        dropPendingPlacement()
        quoteEngine.onAmountChange(value = value)
    }

    override fun onPresetClick(preset: AmountPresetUM) {
        enterAmount(value = preset.amount)
    }

    override fun onMaxClick() {
        val order = uiState.value.order as? OrderSideUM.Buy ?: return
        val max = maxBuyAmount(balance = order.balance ?: return)?.toCollateralInput() ?: return

        enterAmount(value = max)
    }

    private fun enterAmount(value: String) {
        val amount = value.parseBigDecimalOrNull() ?: return
        val current = uiState.value.amountValue.parseBigDecimalOrNull()
        if (orderSubmitter.isSubmitting() || current != null && current.compareTo(amount) == 0) return

        dropPendingPlacement()
        quoteEngine.onAmountChange(value = value, isPicked = true)
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
        if (uiState.value.isPlacementPending) return

        val prepared = quoteEngine.prepare()
        when {
            prepared != null -> place(prepared = prepared)
            uiState.value.isPrimaryButtonEnabledWhileQuoting -> placeOnceQuoted()
        }
    }

    private fun placeOnceQuoted() {
        val tappedAt = timeSource.markNow()
        uiState.update { it.copy(isPlacementPending = true) }

        modelScope.launch {
            uiState.first { it.quote !is QuoteUM.Loading }
            uiState.update { it.copy(isPlacementPending = false) }
            if (tappedAt.elapsedNow() > PENDING_PLACEMENT_LIFETIME) return@launch

            quoteEngine.prepare()?.let { prepared -> place(prepared = prepared) }
        }.saveIn(pendingPlacementJobHolder)
    }

    private fun dropPendingPlacement() {
        pendingPlacementJobHolder.cancel()
        uiState.update { it.copy(isPlacementPending = false) }
    }

    private fun place(prepared: PreparedPrediction) {
        modelScope.launch(start = CoroutineStart.UNDISPATCHED) {
            quoteEngine.holdDuring {
                val result = orderSubmitter.submit(prepared = prepared) ?: return@holdDuring
                router.replaceAll(prepared.toStatusRoute(result = result))
            }
        }
    }

    override fun onPolymarketTermsClick() {
        if (isSubmitting()) return

        urlOpener.openUrl(PolymarketLegalUrls.polymarketTerms)
    }

    override fun onTangemTermsClick() {
        if (isSubmitting()) return

        urlOpener.openUrl(PolymarketLegalUrls.TANGEM_TERMS)
    }

    override fun onCloseClick() {
        if (isSubmitting()) return

        router.pop()
    }

    private fun maxBuyAmount(balance: BigDecimal): BigDecimal? = BuyAmountCalculator.fraction(
        balance = balance,
        fraction = AmountFractionUM.Max,
        feeRate = uiState.value.lastFeeRate,
    ).takeIf { it >= MIN_QUOTABLE_AMOUNT }

    private fun BigDecimal.toCollateralInput(): String =
        parseBigDecimal(decimals = PolymarketConst.COLLATERAL_DISPLAY_DECIMALS, roundingMode = RoundingMode.DOWN)

    private fun toPreset(amount: BigDecimal): AmountPresetUM = AmountPresetUM(
        label = amount.formatPolymarketEnteredAmount(),
        amount = amount.toCollateralInput(),
    )

    private companion object {
        val BUY_PRESET_AMOUNTS = listOf(BigDecimal(100), BigDecimal(500), BigDecimal(1_000), BigDecimal(5_000))
        val PENDING_PLACEMENT_LIFETIME = 10.seconds
    }
}