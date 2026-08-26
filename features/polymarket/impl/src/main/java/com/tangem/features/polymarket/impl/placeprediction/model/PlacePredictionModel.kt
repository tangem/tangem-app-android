package com.tangem.features.polymarket.impl.placeprediction.model

import arrow.core.getOrElse
import com.tangem.core.decompose.di.ModelScoped
import com.tangem.core.decompose.model.Model
import com.tangem.core.decompose.model.ParamsContainer
import com.tangem.core.decompose.navigation.Router
import com.tangem.core.decompose.ui.UiMessageSender
import com.tangem.core.ui.extensions.stringReference
import com.tangem.core.ui.message.DialogMessage
import com.tangem.domain.polymarket.interactor.GetPolymarketBalanceInteractor
import com.tangem.domain.polymarket.model.PredictionOrderQuoteRequest
import com.tangem.domain.polymarket.usecase.DerivePolymarketAddressesUseCase
import com.tangem.domain.polymarket.usecase.CheckPolymarketGeoblockUseCase
import com.tangem.domain.polymarket.usecase.GetPolymarketEventUseCase
import com.tangem.domain.polymarket.usecase.GetPredictionOrderQuoteUseCase
import com.tangem.features.polymarket.impl.placeprediction.PlacePredictionComponent
import com.tangem.features.polymarket.impl.placeprediction.PlacePredictionRoute
import com.tangem.features.polymarket.impl.placeprediction.entity.PlacePredictionUM
import com.tangem.features.polymarket.impl.placeprediction.entity.SubmitUM
import com.tangem.features.polymarket.impl.placeprediction.entity.TradingPermissionUM
import com.tangem.features.polymarket.impl.placeprediction.entity.enteredAmount
import com.tangem.features.polymarket.impl.placeprediction.model.transformers.SetAmountTransformer
import com.tangem.features.polymarket.impl.placeprediction.model.transformers.SetBalanceTransformer
import com.tangem.features.polymarket.impl.placeprediction.model.transformers.SetMarketTransformer
import com.tangem.features.polymarket.impl.placeprediction.model.transformers.SetQuoteLoadingTransformer
import com.tangem.features.polymarket.impl.placeprediction.model.transformers.SetQuoteResultTransformer
import com.tangem.features.polymarket.impl.placeprediction.model.transformers.SetSlippageTransformer
import com.tangem.features.polymarket.impl.placeprediction.model.transformers.SetSubmitTransformer
import com.tangem.features.polymarket.impl.placeprediction.model.transformers.SetTradingPermissionTransformer
import com.tangem.utils.coroutines.CoroutineDispatcherProvider
import com.tangem.utils.coroutines.JobHolder
import com.tangem.utils.coroutines.saveIn
import com.tangem.utils.logging.TangemLogger
import com.tangem.utils.transformer.update
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.isActive
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import java.math.BigDecimal
import javax.inject.Inject

internal const val QUOTE_DEBOUNCE_MILLIS = 500L
internal const val QUOTE_POLL_INTERVAL_MILLIS = 10_000L

/**
 * The only owner of the place-prediction state: every step renders a slice of [uiState] and writes back through
 * [PlacePredictionIntents].
 *
 * The quote is re-requested on an interval rather than fetched once, because the number it carries is the sum that
 * is actually debited, and the book moves. The loop stops as soon as a submission starts, so what is signed is what
 * was shown.
 */
@Suppress("LongParameterList")
@ModelScoped
internal class PlacePredictionModel @Inject constructor(
    paramsContainer: ParamsContainer,
    private val router: Router,
    private val messageSender: UiMessageSender,
    override val dispatchers: CoroutineDispatcherProvider,
    private val getPolymarketEventUseCase: GetPolymarketEventUseCase,
    private val derivePolymarketAddressesUseCase: DerivePolymarketAddressesUseCase,
    private val getPolymarketBalanceInteractor: GetPolymarketBalanceInteractor,
    private val getPredictionOrderQuoteUseCase: GetPredictionOrderQuoteUseCase,
    private val checkPolymarketGeoblockUseCase: CheckPolymarketGeoblockUseCase,
) : Model(), PlacePredictionIntents {

    private val params = paramsContainer.require<PlacePredictionComponent.Params>()

    val uiState: StateFlow<PlacePredictionUM>
        field = MutableStateFlow(PlacePredictionUM.initial())

    private val quoteJobHolder = JobHolder()

    init {
        loadMarket()
        loadBalance()
        loadTradingAllowance()
    }

    /**
     * Whether the region allows trading — read here rather than carried in on the route, because a route
     * cannot know it and a screen that assumes it would be confidently wrong. An unreadable region is
     * treated as blocked: this gate exists to stop an order, so it fails shut.
     */
    private fun loadTradingAllowance() {
        modelScope.launch {
            val isBlocked = withContext(dispatchers.default) {
                checkPolymarketGeoblockUseCase().getOrElse { true }
            }

            uiState.update(
                SetTradingPermissionTransformer(
                    permission = if (isBlocked) TradingPermissionUM.Restricted else TradingPermissionUM.Allowed,
                ),
            )
        }
    }

    override fun onAmountChange(value: String) {
        uiState.update(SetAmountTransformer(value = value))
        restartQuoteLoop(withDebounce = true)
    }

    override fun onSlippageSelected(percent: BigDecimal) {
        uiState.update(SetSlippageTransformer(percent = percent))
        restartQuoteLoop(withDebounce = false)
    }

    override fun onSlippageClick() {
        showComingLater(part = "Choosing the slippage")
    }

    override fun onAddFundsClick() {
        showComingLater(part = "Adding funds")
    }

    override fun onQuoteRetryClick() {
        restartQuoteLoop(withDebounce = false)
    }

    override fun onNextClick() {
        router.push(PlacePredictionRoute.Summary)
    }

    override fun onPlaceClick() {
        uiState.update(SetSubmitTransformer(submit = SubmitUM.Signing))

        messageSender.send(
            DialogMessage(
                title = stringReference(value = "Not implemented"),
                message = stringReference(value = "Signing and placing the order arrive in the next part."),
                onDismissRequest = { uiState.update(SetSubmitTransformer(submit = SubmitUM.Idle)) },
            ),
        )
    }

    private fun showComingLater(part: String) {
        messageSender.send(
            DialogMessage(
                title = stringReference(value = "Not implemented"),
                message = stringReference(value = "$part arrives in a later part of this flow."),
            ),
        )
    }

    override fun onBackClick() {
        router.pop()
    }

    override fun onCloseClick() {
        router.popTo(PlacePredictionRoute.Amount)
        router.pop()
    }

    private fun loadMarket() {
        modelScope.launch {
            val event = withContext(dispatchers.default) {
                getPolymarketEventUseCase(eventId = params.eventId).getOrNull()
            } ?: return@launch

            val market = event.markets.firstOrNull { it.id == params.marketId } ?: return@launch
            val outcome = market.outcomes.firstOrNull { it.assetId == params.assetId } ?: return@launch

            uiState.update(SetMarketTransformer(market = market, outcome = outcome))
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
        modelScope.launch {
            val balance = withContext(dispatchers.default) {
                val addresses = derivePolymarketAddressesUseCase.stored(userWalletId = params.userWalletId)
                    ?: return@withContext null

                getPolymarketBalanceInteractor(addresses = addresses).getOrNull()
            }

            if (balance == null) logger.e("Spending power could not be read; the screen keeps it unknown")

            uiState.update(SetBalanceTransformer(balance = balance?.balance, tokenSymbol = COLLATERAL_SYMBOL))
        }
    }

    private fun restartQuoteLoop(withDebounce: Boolean) {
        quoteJobHolder.cancel()
        if (uiState.value.enteredAmount() == null) return

        modelScope.launch {
            if (withDebounce) delay(timeMillis = QUOTE_DEBOUNCE_MILLIS)

            while (isActive && uiState.value.submit is SubmitUM.Idle) {
                requestQuote()
                delay(timeMillis = QUOTE_POLL_INTERVAL_MILLIS)
            }
        }.saveIn(quoteJobHolder)
    }

    private suspend fun requestQuote() {
        val state = uiState.value
        val amount = state.enteredAmount() ?: return
        if (state.submit !is SubmitUM.Idle) return

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

        if (uiState.value.submit is SubmitUM.Idle) {
            uiState.update(SetQuoteResultTransformer(result = result))
        }
    }

    private companion object {

        val logger = TangemLogger.withTag(tag = "PlacePredictionModel")

        const val COLLATERAL_SYMBOL = "USDC"
    }
}