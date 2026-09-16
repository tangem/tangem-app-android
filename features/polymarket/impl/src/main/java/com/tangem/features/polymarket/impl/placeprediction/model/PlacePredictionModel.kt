package com.tangem.features.polymarket.impl.placeprediction.model

import arrow.core.getOrElse
import com.tangem.core.decompose.di.ModelScoped
import com.tangem.core.decompose.model.Model
import com.tangem.core.decompose.model.ParamsContainer
import com.tangem.core.decompose.navigation.Router
import com.tangem.core.decompose.ui.UiMessageSender
import com.tangem.core.navigation.url.UrlOpener
import com.tangem.core.ui.extensions.TextReference
import com.tangem.core.ui.extensions.resourceReference
import com.tangem.core.ui.extensions.stringReference
import com.tangem.core.ui.message.DialogMessage
import com.tangem.core.ui.message.EventMessageAction
import com.tangem.core.ui.R
import com.tangem.domain.feedback.GetWalletMetaInfoUseCase
import com.tangem.domain.feedback.SendFeedbackEmailUseCase
import com.tangem.domain.feedback.models.FeedbackEmailType
import com.tangem.domain.polymarket.interactor.GetPolymarketBalanceInteractor
import com.tangem.domain.polymarket.interactor.PredictionSignRequest
import com.tangem.domain.polymarket.interactor.SignPredictionOrderInteractor
import com.tangem.domain.polymarket.model.PolymarketAddresses
import com.tangem.domain.polymarket.model.PolymarketSigningError
import com.tangem.domain.polymarket.model.PredictionOrderFailure
import com.tangem.domain.polymarket.model.PredictionOrderQuoteRequest
import com.tangem.domain.polymarket.model.PredictionOrderSide
import com.tangem.domain.polymarket.model.PredictionPriceError
import com.tangem.domain.polymarket.model.PredictionPlaceOrderError
import com.tangem.domain.polymarket.usecase.DerivePolymarketAddressesUseCase
import com.tangem.domain.polymarket.usecase.CheckPolymarketGeoblockUseCase
import com.tangem.domain.polymarket.usecase.GetPolymarketCollateralCurrencyUseCase
import com.tangem.domain.polymarket.usecase.GetPolymarketEventUseCase
import com.tangem.domain.polymarket.usecase.GetPredictionOrderQuoteUseCase
import com.tangem.domain.polymarket.usecase.GetPredictionPriceUseCase
import com.tangem.domain.polymarket.usecase.AwaitDelayedPredictionOrderUseCase
import com.tangem.domain.polymarket.usecase.DelayedPredictionOrderOutcome
import com.tangem.domain.polymarket.usecase.PlacePredictionOrderUseCase
import com.tangem.features.polymarket.impl.common.PolymarketLegalUrls
import com.tangem.features.polymarket.impl.navigation.PolymarketRoute
import com.tangem.features.polymarket.impl.placeprediction.PlacePredictionComponent
import com.tangem.features.polymarket.impl.placeprediction.PlacePredictionRoute
import com.tangem.features.polymarket.impl.placeprediction.entity.PlaceResultUM
import com.tangem.features.polymarket.impl.placeprediction.entity.PlacePredictionUM
import com.tangem.features.polymarket.impl.placeprediction.entity.PriceErrorUM
import com.tangem.features.polymarket.impl.placeprediction.entity.QuoteUM
import com.tangem.features.polymarket.impl.placeprediction.entity.SignableQuoteUM
import com.tangem.features.polymarket.impl.placeprediction.entity.SlippagePickerUM
import com.tangem.features.polymarket.impl.placeprediction.entity.SlippageUM
import com.tangem.features.polymarket.impl.placeprediction.entity.SubmitUM
import com.tangem.features.polymarket.impl.placeprediction.entity.TradingPermissionUM
import com.tangem.features.polymarket.impl.placeprediction.entity.enteredAmount
import com.tangem.features.polymarket.impl.placeprediction.entity.isBelowMinAmount
import com.tangem.features.polymarket.impl.placeprediction.entity.isMarketClosed
import com.tangem.features.polymarket.impl.placeprediction.model.converter.PlaceOutcome
import com.tangem.features.polymarket.impl.placeprediction.model.converter.PlaceResultUMConverter
import com.tangem.features.polymarket.impl.placeprediction.model.transformers.SetAmountTransformer
import com.tangem.features.polymarket.impl.placeprediction.model.transformers.SetBalanceTransformer
import com.tangem.features.polymarket.impl.placeprediction.model.transformers.SetMarketTransformer
import com.tangem.features.polymarket.impl.placeprediction.model.transformers.SetOutcomePriceTransformer
import com.tangem.features.polymarket.impl.placeprediction.model.transformers.SetPaymentCurrencyTransformer
import com.tangem.features.polymarket.impl.placeprediction.model.transformers.SetQuoteLoadingTransformer
import com.tangem.features.polymarket.impl.placeprediction.model.transformers.SetPriceErrorTransformer
import com.tangem.features.polymarket.impl.placeprediction.model.transformers.SetQuoteResultTransformer
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
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.isActive
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import java.math.BigDecimal
import javax.inject.Inject

internal const val QUOTE_DEBOUNCE_MILLIS = 500L

internal const val LIVE_QUOTE_POLL_INTERVAL_MILLIS = 3_000L
internal const val QUOTE_POLL_INTERVAL_MILLIS = 15_000L

@Suppress("LongParameterList", "LargeClass")
@ModelScoped
internal class PlacePredictionModel @Inject constructor(
    paramsContainer: ParamsContainer,
    private val router: Router,
    private val urlOpener: UrlOpener,
    private val getWalletMetaInfoUseCase: GetWalletMetaInfoUseCase,
    private val sendFeedbackEmailUseCase: SendFeedbackEmailUseCase,
    private val messageSender: UiMessageSender,
    override val dispatchers: CoroutineDispatcherProvider,
    private val getPolymarketEventUseCase: GetPolymarketEventUseCase,
    private val getPolymarketCollateralCurrencyUseCase: GetPolymarketCollateralCurrencyUseCase,
    private val derivePolymarketAddressesUseCase: DerivePolymarketAddressesUseCase,
    private val getPolymarketBalanceInteractor: GetPolymarketBalanceInteractor,
    private val getPredictionOrderQuoteUseCase: GetPredictionOrderQuoteUseCase,
    private val getPredictionPriceUseCase: GetPredictionPriceUseCase,
    private val checkPolymarketGeoblockUseCase: CheckPolymarketGeoblockUseCase,
    private val signPredictionOrderInteractor: SignPredictionOrderInteractor,
    private val placePredictionOrderUseCase: PlacePredictionOrderUseCase,
    private val awaitDelayedPredictionOrderUseCase: AwaitDelayedPredictionOrderUseCase,
) : Model(), PlacePredictionIntents {

    private val params = paramsContainer.require<PlacePredictionComponent.Params>()

    val uiState: StateFlow<PlacePredictionUM>
        field = MutableStateFlow(PlacePredictionUM.initial())

    private val quoteJobHolder = JobHolder()

    private val tradingAllowanceJobHolder = JobHolder()

    private var isMarketLive = false

    private var isNegRiskMarket: Boolean? = null

    private var isScreenShown = false

    init {
        uiState.update(SetPaymentCurrencyTransformer(currency = getPolymarketCollateralCurrencyUseCase()))
        loadMarket()
        loadBalance()
        loadTradingAllowance()
    }

    /**
     * Whether the region allows trading — read here rather than carried in on the route, because a route
     * cannot know it and a screen that assumes it would be confidently wrong. An unreadable region keeps the
     * gate shut, but as [TradingPermissionUM.Unknown]: refusing the order is this gate's job, claiming the
     * user is restricted is not.
     */
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

    /**
     * The flow polls a live market every few seconds, so it must not keep doing that off-screen: the loop is
     * suspended with the screen and resumed with it, rather than living as long as the model.
     */
    fun onScreenShown() {
        isScreenShown = true
        if (uiState.value.tradingPermission == TradingPermissionUM.Unknown) loadTradingAllowance()
        restartQuoteLoop(withDebounce = false)
    }

    fun onScreenHidden() {
        isScreenShown = false
        quoteJobHolder.cancel()
    }

    override fun onAmountChange(value: String) {
        uiState.update(SetAmountTransformer(value = value))
        restartQuoteLoop(withDebounce = true)
    }

    override fun onSlippageClick() {
        uiState.update { it.copy(slippagePicker = SlippagePickerUM.of(value = it.slippage.percent.toPlainString())) }
        router.push(PlacePredictionRoute.Slippage)
    }

    override fun onSlippageValueChange(value: String) {
        uiState.update { it.copy(slippagePicker = SlippagePickerUM.of(value = value)) }
    }

    override fun onSlippagePresetClick(percent: BigDecimal) {
        uiState.update { it.copy(slippagePicker = SlippagePickerUM.of(value = percent.toPlainString())) }
    }

    override fun onSlippageConfirmClick() {
        val percent = uiState.value.slippagePicker.value.toBigDecimalOrNull() ?: return

        uiState.update { it.copy(slippage = SlippageUM.of(percent = percent)) }
        restartQuoteLoop(withDebounce = false)
        router.pop()
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
        val state = uiState.value
        if (state.submit !is SubmitUM.Idle) return

        val frozen = (state.quote as? QuoteUM.Content)?.signable
        if (frozen == null) {
            showPlaceFailure(reason = "Place tapped without a priced quote")
            return
        }

        uiState.update(SetSubmitTransformer(submit = SubmitUM.Signing))
        modelScope.launch { place(frozen = frozen) }
    }

    private suspend fun place(frozen: SignableQuoteUM) {
        val isNegRisk = isNegRiskMarket
        val addresses = withContext(dispatchers.default) {
            derivePolymarketAddressesUseCase.stored(userWalletId = params.userWalletId)
        }
        if (addresses == null || isNegRisk == null) {
            showPlaceFailure(reason = "No stored addresses or unknown market kind")
            return
        }

        when (spendingPower(addresses = addresses, required = frozen.quote.total)) {
            SpendingPower.Sufficient -> Unit
            SpendingPower.Insufficient -> {
                returnToIdle()
                return
            }
            SpendingPower.Unknown -> {
                showPlaceFailure(reason = "Spending power could not be read")
                return
            }
        }

        val signedOrder = withContext(dispatchers.default) {
            signPredictionOrderInteractor(
                request = PredictionSignRequest(
                    addresses = addresses,
                    quote = frozen.quote,
                    assetId = params.assetId,
                    side = params.side,
                    isNegRisk = isNegRisk,
                    quoteReceivedAtMillis = frozen.receivedAtMillis,
                ),
            )
        }.getOrElse { failure ->
            onSignFailure(failure = failure)
            return
        }

        uiState.update(SetSubmitTransformer(submit = SubmitUM.Posting))

        withContext(dispatchers.default) {
            placePredictionOrderUseCase(addresses = addresses, signedOrder = signedOrder, isNegRisk = isNegRisk)
        }
            .map { result ->
                PlaceResultUMConverter.convert(
                    result = result,
                    quotedShares = frozen.quote.shares,
                    quotedAmount = frozen.quote.notional,
                    placedAt = System.currentTimeMillis(),
                )
            }
            .fold(
                ifLeft = ::onPlaceError,
                ifRight = { outcome ->
                    onPlaceOutcome(outcome = outcome, addresses = addresses, quotedAmount = frozen.quote.notional)
                },
            )
    }

    private suspend fun spendingPower(addresses: PolymarketAddresses, required: BigDecimal): SpendingPower {
        val balance = withContext(dispatchers.default) {
            getPolymarketBalanceInteractor(addresses = addresses).getOrNull()
        } ?: return SpendingPower.Unknown

        uiState.update(SetBalanceTransformer(balance = balance.balance))

        return if (balance.balance >= required) SpendingPower.Sufficient else SpendingPower.Insufficient
    }

    private enum class SpendingPower { Sufficient, Insufficient, Unknown }

    private fun onSignFailure(failure: PredictionOrderFailure) {
        val isCancelled = failure is PredictionOrderFailure.Signing &&
            failure.error == PolymarketSigningError.UserCancelled

        if (isCancelled) {
            returnToIdle()
        } else {
            showPlaceFailure(reason = failure.toReason())
        }
    }

    private fun PredictionOrderFailure.toReason(): String = when (this) {
        is PredictionOrderFailure.Draft -> "Order could not be drafted: $error"
        is PredictionOrderFailure.Invalid -> "Order failed its own checks: $error"
        is PredictionOrderFailure.Signing -> "Signing failed: $error"
    }

    private fun onPlaceError(error: PredictionPlaceOrderError) {
        when (error) {
            is PredictionPlaceOrderError.Ambiguous -> {
                logger.e("Order outcome unknown, reporting it as submitted: $error")
                showResult(result = PlaceResultUM.Submitted(placedAt = System.currentTimeMillis()))
            }
            is PredictionPlaceOrderError.NoLiquidity -> {
                logger.e("The exchange found no match for the order: $error")
                showResult(result = PlaceResultUM.NotFilled(placedAt = System.currentTimeMillis()))
            }
            else -> showPlaceFailure(reason = "Order refused: $error")
        }
    }

    private suspend fun onPlaceOutcome(
        outcome: PlaceOutcome,
        addresses: PolymarketAddresses,
        quotedAmount: BigDecimal,
    ) {
        when (outcome) {
            is PlaceOutcome.Reportable -> showResult(result = outcome.result)
            is PlaceOutcome.Anomalous -> showPlaceFailure(reason = outcome.reason)
            is PlaceOutcome.Delayed -> awaitDelayedFill(
                addresses = addresses,
                orderId = outcome.orderId,
                quotedAmount = quotedAmount,
                placedAt = outcome.placedAt,
            )
        }
    }

    /**
     * The exchange accepted the order into a matching delay, so the answer to "did it fill" is read back from
     * the book. A match ends on the result screen like an immediate one; anything else is a dialog over the
     * summary, since the order did not place and the same one can be placed again.
     */
    private suspend fun awaitDelayedFill(
        addresses: PolymarketAddresses,
        orderId: String,
        quotedAmount: BigDecimal,
        placedAt: Long,
    ) {
        uiState.update(SetSubmitTransformer(submit = SubmitUM.AwaitingFill))

        val outcome = withContext(dispatchers.default) {
            awaitDelayedPredictionOrderUseCase(addresses = addresses, orderId = orderId)
        }

        when (outcome) {
            is DelayedPredictionOrderOutcome.Matched -> {
                val result = PlaceResultUMConverter.convert(
                    order = outcome.order,
                    quotedAmount = quotedAmount,
                    placedAt = placedAt,
                )
                if (result == null) {
                    showPlaceFailure(reason = "Delayed order $orderId matched, but its fill cannot be read")
                } else {
                    showResult(result = result)
                }
            }
            is DelayedPredictionOrderOutcome.Failed -> {
                showPlaceFailure(reason = "Delayed order $orderId did not fill: ${outcome.detail}")
            }
            is DelayedPredictionOrderOutcome.Unconfirmed -> {
                logger.e("Delayed order $orderId: the book did not say whether it matched within the reads allowed")
                returnToIdle()
                showFailureDialog(
                    title = resourceReference(R.string.prediction_place_delayed_timeout_title),
                    message = resourceReference(R.string.prediction_place_delayed_timeout_description),
                )
            }
        }
    }

    private fun showResult(result: PlaceResultUM) {
        uiState.update(SetSubmitTransformer(submit = SubmitUM.Result(result = result)))
        router.replaceAll(PlacePredictionRoute.Status)
    }

    /**
     * A submission that never reached the book: the same order can still be placed, so this is a dialog over
     * the summary rather than a result screen, and it returns the flow to [SubmitUM.Idle].
     */
    fun showPlaceFailure(reason: String) {
        logger.e(reason)
        returnToIdle()

        showFailureDialog(
            title = resourceReference(R.string.common_something_went_wrong),
            message = resourceReference(R.string.prediction_place_error_description),
        )
    }

    private fun showFailureDialog(title: TextReference, message: TextReference) {
        messageSender.send(
            DialogMessage(
                title = title,
                message = message,
                firstActionBuilder = {
                    EventMessageAction(
                        title = resourceReference(R.string.common_support),
                        onClick = ::onSupportClick,
                    )
                },
                secondActionBuilder = { cancelAction() },
            ),
        )
    }

    private fun returnToIdle() {
        uiState.update(SetSubmitTransformer(submit = SubmitUM.Idle))

        if (isScreenShown) restartQuoteLoop(withDebounce = false)
    }

    private fun onSupportClick() {
        modelScope.launch {
            val metaInfo = getWalletMetaInfoUseCase(userWalletId = params.userWalletId).getOrNull() ?: return@launch

            sendFeedbackEmailUseCase(type = FeedbackEmailType.DirectUserRequest(walletMetaInfo = metaInfo))
        }
    }

    private fun showComingLater(part: String) {
        messageSender.send(
            DialogMessage(
                title = stringReference(value = "Not implemented"),
                message = stringReference(value = "$part arrives in a later part of this flow."),
            ),
        )
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

    override fun onToMainClick() {
        router.popTo(PolymarketRoute.Main(userWalletId = params.userWalletId))
    }

    fun isSubmitting(): Boolean = when (uiState.value.submit) {
        is SubmitUM.Signing,
        is SubmitUM.Posting,
        is SubmitUM.AwaitingFill,
        -> true
        is SubmitUM.Idle,
        is SubmitUM.Result,
        -> false
    }

    private fun loadMarket() {
        modelScope.launch {
            val event = withContext(dispatchers.default) {
                getPolymarketEventUseCase(eventId = params.eventId).getOrNull()
            } ?: return@launch

            val market = event.markets.firstOrNull { it.id == params.marketId } ?: return@launch
            val outcome = market.outcomes.firstOrNull { it.assetId == params.assetId } ?: return@launch

            isNegRiskMarket = market.isNegRisk

            uiState.update(SetMarketTransformer(market = market, outcome = outcome))

            if (isScreenShown) restartQuoteLoop(withDebounce = false)
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

            uiState.update(SetBalanceTransformer(balance = balance?.balance))
        }
    }

    private fun restartQuoteLoop(withDebounce: Boolean) {
        quoteJobHolder.cancel()

        if (!uiState.value.isMarketAcceptingOrders) return

        if (uiState.value.enteredAmount() == null) {
            startPriceLoop()
            return
        }

        if (uiState.value.isBelowMinAmount()) return

        modelScope.launch {
            if (withDebounce) delay(timeMillis = QUOTE_DEBOUNCE_MILLIS)

            while (isActive && uiState.value.submit is SubmitUM.Idle) {
                requestQuote()
                if (uiState.value.quote.isMarketClosed()) break
                delay(timeMillis = pollInterval())
            }
        }.saveIn(quoteJobHolder)
    }

    private fun startPriceLoop() {
        modelScope.launch {
            while (isActive && uiState.value.submit is SubmitUM.Idle && uiState.value.enteredAmount() == null) {
                val canKeepPolling = requestPrice()
                if (!canKeepPolling) return@launch
                delay(timeMillis = pollInterval())
            }
        }.saveIn(quoteJobHolder)
    }

    private suspend fun requestPrice(): Boolean {
        val result = withContext(dispatchers.default) {
            // A buy executes against the SELL side of the book, so the fair price to show is the best ask.
            getPredictionPriceUseCase(assetId = params.assetId, side = PredictionOrderSide.SELL)
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

    private fun pollInterval(): Long = if (isMarketLive) LIVE_QUOTE_POLL_INTERVAL_MILLIS else QUOTE_POLL_INTERVAL_MILLIS

    private suspend fun requestQuote() {
        val state = uiState.value
        val amount = state.enteredAmount() ?: return
        if (state.submit !is SubmitUM.Idle || state.isBelowMinAmount()) return

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

        result.onRight { isMarketLive = it.isLive }
        if (uiState.value.submit is SubmitUM.Idle) {
            uiState.update(
                SetQuoteResultTransformer(result = result, receivedAtMillis = System.currentTimeMillis()),
            )
        }
    }

    private companion object {

        val logger = TangemLogger.withTag(tag = "PlacePredictionModel")
    }
}