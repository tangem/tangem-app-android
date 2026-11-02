package com.tangem.features.polymarket.impl.placeprediction.model

import com.arkivanov.decompose.router.slot.SlotNavigation
import com.tangem.core.decompose.di.ModelScoped
import com.tangem.core.decompose.model.Model
import com.tangem.core.decompose.model.ParamsContainer
import com.tangem.core.decompose.navigation.Router
import com.tangem.core.navigation.url.UrlOpener
import com.tangem.core.ui.utils.parseBigDecimal
import com.tangem.core.ui.utils.parseBigDecimalOrNull
import com.tangem.domain.models.wallet.isHotWallet
import com.tangem.domain.polymarket.usecase.CanSignPredictionOrderWithSessionKeyUseCase
import com.tangem.domain.wallets.usecase.GetUserWalletUseCase
import com.tangem.features.polymarket.api.PolymarketFeatureToggles
import com.tangem.features.polymarket.impl.common.PolymarketConst
import com.tangem.features.polymarket.impl.common.PolymarketLegalUrls
import com.tangem.features.polymarket.impl.common.formatPolymarketEnteredAmount
import com.tangem.features.polymarket.impl.main.addfunds.PolymarketAddFundsComponent
import com.tangem.features.polymarket.impl.main.addfunds.PolymarketFundingSheetConfig
import com.tangem.features.polymarket.impl.placeprediction.PlacePredictionComponent
import com.tangem.features.polymarket.impl.placeprediction.PlacePredictionRoute
import com.tangem.features.polymarket.impl.placeprediction.entity.AmountFractionUM
import com.tangem.features.polymarket.impl.placeprediction.entity.AmountPresetUM
import com.tangem.features.polymarket.impl.placeprediction.entity.MIN_QUOTABLE_AMOUNT
import com.tangem.features.polymarket.impl.placeprediction.entity.OrderSideUM
import com.tangem.features.polymarket.impl.placeprediction.entity.PlaceActionUM
import com.tangem.features.polymarket.impl.placeprediction.entity.PlaceButtonStyleUM
import com.tangem.features.polymarket.impl.placeprediction.entity.PlacePredictionUM
import com.tangem.features.polymarket.impl.placeprediction.entity.PreparedPrediction
import com.tangem.features.polymarket.impl.placeprediction.entity.PredictionFeeSheetUM
import com.tangem.features.polymarket.impl.placeprediction.entity.SubmitUM
import com.tangem.features.polymarket.impl.placeprediction.entity.toStatusRoute
import com.tangem.features.polymarket.impl.placeprediction.model.transformers.SetSubmitTransformer
import com.tangem.utils.coroutines.CoroutineDispatcherProvider
import com.tangem.utils.transformer.update
import kotlinx.collections.immutable.ImmutableList
import kotlinx.collections.immutable.persistentListOf
import kotlinx.collections.immutable.toImmutableList
import kotlinx.coroutines.CoroutineStart
import kotlinx.coroutines.Job
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import kotlinx.coroutines.withTimeoutOrNull
import java.math.BigDecimal
import java.math.RoundingMode
import javax.inject.Inject
import kotlin.time.Duration.Companion.seconds

@Suppress("TooManyFunctions", "LongParameterList")
@ModelScoped
internal class PlacePredictionOrderModel @Inject constructor(
    paramsContainer: ParamsContainer,
    private val router: Router,
    private val urlOpener: UrlOpener,
    override val dispatchers: CoroutineDispatcherProvider,
    quoteEngineFactory: PlacePredictionQuoteEngine.Factory,
    orderSubmitterFactory: PlacePredictionOrderSubmitter.Factory,
    private val getUserWalletUseCase: GetUserWalletUseCase,
    private val featureToggles: PolymarketFeatureToggles,
    private val canSignPredictionOrderWithSessionKeyUseCase: CanSignPredictionOrderWithSessionKeyUseCase,
) : Model(), PlacePredictionOrderIntents, PolymarketAddFundsComponent.ModelCallback {

    private val params = paramsContainer.require<PlacePredictionComponent.Params>()

    val uiState: StateFlow<PlacePredictionUM>
        field = MutableStateFlow(PlacePredictionUM.initial(side = params.side))

    private val quoteEngine = quoteEngineFactory.create(params = params, uiState = uiState, scope = modelScope)

    private val orderSubmitter = orderSubmitterFactory.create(params = params, uiState = uiState, scope = modelScope)

    private var pendingPlacement: Job? = null

    private var sessionKeyCheck: Job? = null

    private var isCardWallet = false

    private var sessionKeyStyle: PlaceButtonStyleUM? = null

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

    init {
        resolveWalletType()
    }

    private fun resolveWalletType() {
        getUserWalletUseCase(params.userWalletId).onRight { userWallet ->
            if (userWallet.isHotWallet) {
                uiState.update { it.copy(placeButtonStyle = PlaceButtonStyleUM.HOLD) }
                return@onRight
            }

            isCardWallet = true
            uiState.update { it.copy(placeButtonStyle = PlaceButtonStyleUM.CARD) }
        }
    }

    private fun checkSessionKey() {
        if (!isCardWallet || !featureToggles.isPolymarketSessionKeysEnabled) return

        sessionKeyCheck?.cancel()
        sessionKeyCheck = modelScope.launch {
            val canSign = withContext(dispatchers.io) {
                canSignPredictionOrderWithSessionKeyUseCase(userWalletId = params.userWalletId)
            }
            sessionKeyStyle = if (canSign) PlaceButtonStyleUM.PLAIN else PlaceButtonStyleUM.CARD
            applySessionKeyStyleIfIdle()
        }
    }

    private fun applySessionKeyStyleIfIdle() {
        val style = sessionKeyStyle ?: return
        uiState.update { state -> if (state.isIdle()) state.copy(placeButtonStyle = style) else state }
    }

    fun onResume() {
        quoteEngine.onResume()
        checkSessionKey()
    }

    fun onPause() {
        dropPendingPlacement()
        quoteEngine.onPause()
    }

    fun isSubmitting(): Boolean = orderSubmitter.isSubmitting() || uiState.value.submit is SubmitUM.AwaitingQuote

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
        when (uiState.value.placeAction) {
            PlaceActionUM.Ready -> quoteEngine.prepare()?.let(::place) ?: settleButtonIfIdle()
            PlaceActionUM.ReadyOnceQuoted -> placeOnceQuoted()
            PlaceActionUM.AwaitingQuote -> resetHold()
            PlaceActionUM.Unavailable -> settleButtonIfIdle()
        }
    }

    private fun placeOnceQuoted() {
        val wait = modelScope.launch {
            when (withTimeoutOrNull(PENDING_PLACEMENT_LIFETIME) { quoteEngine.awaitFreshQuote() }) {
                true -> {
                    releaseTap()
                    quoteEngine.prepare()?.let(::place)
                }
                null -> quoteEngine.abandonFreshQuote()
                false -> Unit
            }
        }

        pendingPlacement = wait
        uiState.update(SetSubmitTransformer(submit = SubmitUM.AwaitingQuote))
        wait.invokeOnCompletion { if (pendingPlacement === wait) dropPendingPlacement() }
    }

    private fun dropPendingPlacement() {
        val wait = pendingPlacement ?: return
        pendingPlacement = null
        wait.cancel()
        releaseTap()
        settleButtonIfIdle()
    }

    private fun releaseTap() {
        if (uiState.value.submit is SubmitUM.AwaitingQuote) {
            uiState.update(SetSubmitTransformer(submit = SubmitUM.Idle))
        }
    }

    private fun settleButtonIfIdle() {
        if (!uiState.value.isIdle()) return

        resetHold()
        applySessionKeyStyleIfIdle()
    }

    private fun PlacePredictionUM.isIdle(): Boolean = submit is SubmitUM.Idle

    private fun resetHold() {
        uiState.update { it.copy(holdResetKey = it.holdResetKey + 1) }
    }

    private fun place(prepared: PreparedPrediction) {
        modelScope.launch(start = CoroutineStart.UNDISPATCHED) {
            quoteEngine.holdDuring {
                val result = orderSubmitter.submit(prepared = prepared)
                if (result == null) {
                    settleButtonIfIdle()
                    return@holdDuring
                }
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