package com.tangem.features.polymarket.impl.placeprediction.model

import com.tangem.core.decompose.di.ModelScoped
import com.tangem.core.decompose.model.Model
import com.tangem.core.decompose.model.ParamsContainer
import com.tangem.core.decompose.navigation.Router
import com.tangem.core.decompose.ui.UiMessageSender
import com.tangem.core.ui.extensions.stringReference
import com.tangem.core.ui.message.DialogMessage
import com.tangem.domain.polymarket.PolymarketPositionsRepository
import com.tangem.domain.polymarket.interactor.GetPolymarketBalanceInteractor
import com.tangem.domain.polymarket.model.PolymarketPositionId
import com.tangem.domain.polymarket.model.PredictionOrderQuoteRequest
import com.tangem.domain.polymarket.model.PredictionPriceError
import com.tangem.domain.polymarket.usecase.CheckPolymarketGeoblockUseCase
import com.tangem.domain.polymarket.usecase.DerivePolymarketAddressesUseCase
import com.tangem.domain.polymarket.usecase.GetPolymarketCollateralCurrencyUseCase
import com.tangem.domain.polymarket.usecase.GetPolymarketEventUseCase
import com.tangem.domain.polymarket.usecase.GetPredictionOrderQuoteUseCase
import com.tangem.domain.polymarket.usecase.GetPredictionPriceUseCase
import com.tangem.features.polymarket.impl.placeprediction.PlacePredictionComponent
import com.tangem.features.polymarket.impl.placeprediction.PlacePredictionRoute
import com.tangem.features.polymarket.impl.placeprediction.entity.OrderSideUM
import com.tangem.features.polymarket.impl.placeprediction.entity.PlacePredictionUM
import com.tangem.features.polymarket.impl.placeprediction.entity.PreparedPrediction
import com.tangem.features.polymarket.impl.placeprediction.entity.PriceErrorUM
import com.tangem.features.polymarket.impl.placeprediction.entity.SellPositionUM
import com.tangem.features.polymarket.impl.placeprediction.entity.SlippageUM
import com.tangem.features.polymarket.impl.placeprediction.entity.TradingPermissionUM
import com.tangem.features.polymarket.impl.placeprediction.entity.canPollPrice
import com.tangem.features.polymarket.impl.placeprediction.entity.enteredAmount
import com.tangem.features.polymarket.impl.placeprediction.entity.isMarketClosed
import com.tangem.features.polymarket.impl.placeprediction.entity.isQuotable
import com.tangem.features.polymarket.impl.placeprediction.model.transformers.SetAmountTransformer
import com.tangem.features.polymarket.impl.placeprediction.model.transformers.SetBalanceTransformer
import com.tangem.features.polymarket.impl.placeprediction.model.transformers.SetCollateralTransformer
import com.tangem.features.polymarket.impl.placeprediction.model.transformers.SetMarketTransformer
import com.tangem.features.polymarket.impl.placeprediction.model.transformers.SetOutcomePriceTransformer
import com.tangem.features.polymarket.impl.placeprediction.model.transformers.SetPositionTransformer
import com.tangem.features.polymarket.impl.placeprediction.model.transformers.SetPriceErrorTransformer
import com.tangem.features.polymarket.impl.placeprediction.model.transformers.SetQuoteLoadingTransformer
import com.tangem.features.polymarket.impl.placeprediction.model.transformers.SetQuoteResultTransformer
import com.tangem.features.polymarket.impl.placeprediction.model.transformers.SetSellPositionStateTransformer
import com.tangem.features.polymarket.impl.placeprediction.model.transformers.SetTradingPermissionTransformer
import com.tangem.utils.coroutines.CoroutineDispatcherProvider
import com.tangem.utils.coroutines.JobHolder
import com.tangem.utils.coroutines.saveIn
import com.tangem.utils.logging.TangemLogger
import com.tangem.utils.transformer.update
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.isActive
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import java.math.BigDecimal
import javax.inject.Inject

internal const val QUOTE_DEBOUNCE_MILLIS = 500L
internal const val QUOTE_POLL_INTERVAL_MILLIS = 3_000L

@Suppress("LongParameterList")
@ModelScoped
internal class PlacePredictionAmountModel @Inject constructor(
    paramsContainer: ParamsContainer,
    private val router: Router,
    private val messageSender: UiMessageSender,
    override val dispatchers: CoroutineDispatcherProvider,
    private val getPolymarketEventUseCase: GetPolymarketEventUseCase,
    private val getPolymarketCollateralCurrencyUseCase: GetPolymarketCollateralCurrencyUseCase,
    private val derivePolymarketAddressesUseCase: DerivePolymarketAddressesUseCase,
    private val getPolymarketBalanceInteractor: GetPolymarketBalanceInteractor,
    private val getPredictionOrderQuoteUseCase: GetPredictionOrderQuoteUseCase,
    private val getPredictionPriceUseCase: GetPredictionPriceUseCase,
    private val checkPolymarketGeoblockUseCase: CheckPolymarketGeoblockUseCase,
    private val positionsRepository: PolymarketPositionsRepository,
) : Model(), PlacePredictionAmountIntents {

    private val params = paramsContainer.require<PlacePredictionComponent.Params>()

    val uiState: StateFlow<PlacePredictionUM>
        field = MutableStateFlow(PlacePredictionUM.initial(side = params.side))

    private val quoteJobHolder = JobHolder()
    private val tradingAllowanceJobHolder = JobHolder()
    private val positionJobHolder = JobHolder()
    private val balanceJobHolder = JobHolder()

    private var isNegRiskMarket: Boolean? = null

    private var positionId: PolymarketPositionId? = null

    private var isResumed = false

    init {
        uiState.update(SetCollateralTransformer(currency = getPolymarketCollateralCurrencyUseCase()))
        when (val params = params) {
            is PlacePredictionComponent.Params.Buy -> loadMarket(params = params)
            is PlacePredictionComponent.Params.Sell -> loadPosition(params = params)
        }
        loadBalance()
        loadTradingAllowance()
    }

    private fun loadTradingAllowance() {
        if (tradingAllowanceJobHolder.isActive) return

        modelScope.launch {
            val permission = withContext(dispatchers.default) {
                checkPolymarketGeoblockUseCase().fold(
                    ifLeft = { TradingPermissionUM.Unknown },
                    ifRight = { isBlocked ->
                        if (isBlocked) TradingPermissionUM.Restricted else TradingPermissionUM.Allowed
                    },
                )
            }

            uiState.update(SetTradingPermissionTransformer(permission = permission))
        }.saveIn(tradingAllowanceJobHolder)
    }

    fun onResume() {
        isResumed = true
        if (uiState.value.tradingPermission == TradingPermissionUM.Unknown) loadTradingAllowance()
        loadBalance()
        if (params is PlacePredictionComponent.Params.Sell) loadPosition(params = params)
        restartQuoteLoop(withDebounce = false)
    }

    fun onPause() {
        isResumed = false
        quoteJobHolder.cancel()
    }

    override fun onAmountChange(value: String) {
        uiState.update(SetAmountTransformer(value = value))
        restartQuoteLoop(withDebounce = true)
    }

    override fun onSlippageClick() {
        router.push(
            PlacePredictionRoute.Slippage(
                value = uiState.value.slippage.percent,
                onConfirm = ::applySlippage,
            ),
        )
    }

    private fun applySlippage(percent: BigDecimal) {
        if (uiState.value.slippage.percent.compareTo(percent) == 0) return

        uiState.update { state ->
            SetAmountTransformer(value = state.amountValue)
                .transform(state.copy(slippage = SlippageUM.of(percent = percent)))
        }
        restartQuoteLoop(withDebounce = false)
    }

    override fun onAddFundsClick() {
        showComingLater(part = "Adding funds")
    }

    override fun onQuoteRetryClick() {
        if (reloadPositionIfLost()) return

        restartQuoteLoop(withDebounce = false)
    }

    override fun onNextClick() {
        val state = uiState.value
        val isNegRisk = isNegRiskMarket ?: return
        if (!state.isPrimaryButtonEnabled) return

        val order = PreparedPrediction(params = params, state = state, isNegRisk = isNegRisk, positionId = positionId)
        quoteJobHolder.cancel()
        router.push(PlacePredictionRoute.Summary(order = order))
    }

    override fun onBackClick() {
        router.pop()
    }

    override fun onCloseClick() {
        router.popTo(PlacePredictionRoute.Amount)
        router.pop()
    }

    private fun showComingLater(part: String) {
        messageSender.send(
            DialogMessage(
                title = stringReference(value = "Not implemented"),
                message = stringReference(value = "$part arrives in a later part of this flow."),
            ),
        )
    }

    private fun loadMarket(params: PlacePredictionComponent.Params.Buy) {
        modelScope.launch {
            val event = withContext(dispatchers.default) {
                getPolymarketEventUseCase(eventId = params.eventId).getOrNull()
            } ?: return@launch

            val market = event.markets.firstOrNull { it.id == params.marketId } ?: return@launch
            val outcome = market.outcomes.firstOrNull { it.assetId == params.assetId } ?: return@launch

            isNegRiskMarket = market.isNegRisk
            positionId = market.conditionId?.let { PolymarketPositionId(conditionId = it, assetId = params.assetId) }

            uiState.update(SetMarketTransformer(market = market, outcome = outcome))

            restartQuoteLoop(withDebounce = false)
        }
    }

    /**
     * The CLOB serves a balance it recomputes only when asked, so the read behind this refreshes it first: the
     * account status cached for the wallet screen is not honest spending power at the moment of an order.
     *
     * A read that fails leaves the balance unknown rather than zero. Zero is an answer — it says the money is
     * spent — and the screen would repeat it as one, refusing the order and blaming the user's funds for what
     * is our own failure to read them.
     */
    private fun loadBalance() {
        if (balanceJobHolder.isActive) return

        modelScope.launch {
            val balance = withContext(dispatchers.default) {
                val addresses = derivePolymarketAddressesUseCase.stored(userWalletId = params.userWalletId)
                    ?: return@withContext null

                getPolymarketBalanceInteractor(addresses = addresses).getOrNull()
            }

            if (balance == null) logger.e("Spending power could not be read; the screen keeps it unknown")

            uiState.update(SetBalanceTransformer(balance = balance?.balance))
        }.saveIn(balanceJobHolder)
    }

    /** @return whether a fresh read was started, in which case it — not the caller — restarts the loop. */
    private fun reloadPositionIfLost(): Boolean {
        val params = params as? PlacePredictionComponent.Params.Sell ?: return false
        val position = (uiState.value.order as? OrderSideUM.Sell)?.position ?: return false

        if (position is SellPositionUM.Loading || position is SellPositionUM.Held) return false

        uiState.update(SetSellPositionStateTransformer(position = SellPositionUM.Loading))
        loadPosition(params = params)
        return true
    }

    private fun failPosition(position: SellPositionUM) {
        uiState.update(SetSellPositionStateTransformer(position = position))
        restartQuoteLoop(withDebounce = false)
    }

    private fun loadPosition(params: PlacePredictionComponent.Params.Sell) {
        if (positionJobHolder.isActive) return

        modelScope.launch {
            val addresses = withContext(dispatchers.default) {
                derivePolymarketAddressesUseCase.stored(userWalletId = params.userWalletId)
            }
            if (addresses == null) {
                logger.e("No stored addresses; the screen keeps nothing sellable")
                failPosition(position = SellPositionUM.Unreadable)
                return@launch
            }

            val position = withContext(dispatchers.default) {
                positionsRepository.getPosition(addresses = addresses, id = params.positionId)
            }.fold(
                ifLeft = { error ->
                    logger.e("Position could not be read, the screen keeps nothing sellable: $error")
                    failPosition(position = SellPositionUM.Unreadable)
                    return@launch
                },
                ifRight = { position ->
                    position ?: run {
                        logger.e("Position is not held or not indexed yet; the screen keeps nothing sellable")
                        failPosition(position = SellPositionUM.NotHeld)
                        return@launch
                    }
                },
            )

            isNegRiskMarket = position.isNegRisk

            uiState.update(SetPositionTransformer(position = position))

            restartQuoteLoop(withDebounce = false)
        }.saveIn(positionJobHolder)
    }

    private fun restartQuoteLoop(withDebounce: Boolean) {
        quoteJobHolder.cancel()

        if (!isResumed || !uiState.value.isMarketAcceptingOrders || !uiState.value.canPollPrice()) return

        modelScope.launch {
            if (withDebounce && uiState.value.isQuotable()) delay(timeMillis = QUOTE_DEBOUNCE_MILLIS)

            while (isActive) {
                val canKeepPolling = if (uiState.value.isQuotable()) {
                    requestQuote()
                    !uiState.value.quote.isMarketClosed()
                } else {
                    requestPrice()
                }
                if (!canKeepPolling) return@launch
                delay(timeMillis = QUOTE_POLL_INTERVAL_MILLIS)
            }
        }.saveIn(quoteJobHolder)
    }

    private suspend fun requestPrice(): Boolean {
        val result = withContext(dispatchers.default) {
            getPredictionPriceUseCase(assetId = params.assetId, side = params.side.opposite())
        }

        return result.fold(
            ifLeft = { error ->
                when (error) {
                    PredictionPriceError.MarketClosed -> {
                        uiState.update(SetPriceErrorTransformer(error = PriceErrorUM.MarketClosed))
                        false
                    }
                    PredictionPriceError.InvalidAsset -> {
                        uiState.update(SetPriceErrorTransformer(error = PriceErrorUM.Unknown))
                        false
                    }
                    PredictionPriceError.EmptyBook -> {
                        uiState.update(SetPriceErrorTransformer(error = PriceErrorUM.EmptyBook))
                        true
                    }
                    PredictionPriceError.Network,
                    is PredictionPriceError.Unknown,
                    -> true
                }
            },
            ifRight = { price ->
                uiState.update(SetOutcomePriceTransformer(price = price))
                true
            },
        )
    }

    private suspend fun requestQuote() {
        val state = uiState.value
        val amount = state.enteredAmount() ?: return
        if (!state.isQuotable()) return

        uiState.update(SetQuoteLoadingTransformer)

        val result = withContext(dispatchers.default) {
            getPredictionOrderQuoteUseCase(
                request = PredictionOrderQuoteRequest(
                    marketId = params.marketId,
                    assetId = params.assetId,
                    side = params.side,
                    amount = amount,
                    slippagePercent = state.slippage.percent,
                ),
            )
        }

        uiState.update(
            SetQuoteResultTransformer(result = result, receivedAtMillis = System.currentTimeMillis()),
        )
    }

    private companion object {
        val logger = TangemLogger.withTag(tag = "PlacePredictionAmountModel")
    }
}