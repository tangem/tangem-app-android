package com.tangem.features.polymarket.impl.placeprediction.model

import arrow.core.getOrElse
import com.arkivanov.decompose.router.slot.SlotNavigation
import com.arkivanov.decompose.router.slot.activate
import com.arkivanov.decompose.router.slot.dismiss
import com.tangem.core.decompose.di.ModelScoped
import com.tangem.core.decompose.model.Model
import com.tangem.core.decompose.model.ParamsContainer
import com.tangem.core.decompose.navigation.Router
import com.tangem.core.decompose.ui.UiMessageSender
import com.tangem.core.navigation.url.UrlOpener
import com.tangem.core.ui.R
import com.tangem.core.ui.extensions.TextReference
import com.tangem.core.ui.extensions.resourceReference
import com.tangem.core.ui.message.DialogMessage
import com.tangem.core.ui.message.EventMessageAction
import com.tangem.domain.feedback.GetWalletMetaInfoUseCase
import com.tangem.domain.feedback.SendFeedbackEmailUseCase
import com.tangem.domain.feedback.models.FeedbackEmailType
import com.tangem.domain.polymarket.PolymarketPositionsRepository
import com.tangem.domain.polymarket.flow.PredictionFundingWindow
import com.tangem.domain.polymarket.interactor.GetPolymarketBalanceInteractor
import com.tangem.domain.polymarket.interactor.PredictionSignRequest
import com.tangem.domain.polymarket.interactor.SignPredictionOrderInteractor
import com.tangem.domain.polymarket.model.PolymarketAddresses
import com.tangem.domain.polymarket.model.PolymarketPositionId
import com.tangem.domain.polymarket.model.PolymarketSigningError
import com.tangem.domain.polymarket.model.PredictionOrderFailure
import com.tangem.domain.polymarket.model.PredictionPlaceOrderError
import com.tangem.domain.polymarket.usecase.AwaitDelayedPredictionOrderUseCase
import com.tangem.domain.polymarket.usecase.DelayedPredictionOrderOutcome
import com.tangem.domain.polymarket.usecase.DerivePolymarketAddressesUseCase
import com.tangem.domain.polymarket.usecase.PlacePredictionOrderUseCase
import com.tangem.features.polymarket.impl.common.PolymarketLegalUrls
import com.tangem.features.polymarket.impl.placeprediction.PlacePredictionComponent
import com.tangem.features.polymarket.impl.placeprediction.PlacePredictionRoute
import com.tangem.features.polymarket.impl.placeprediction.entity.PlacePredictionUM
import com.tangem.features.polymarket.impl.placeprediction.entity.PlaceResultUM
import com.tangem.features.polymarket.impl.placeprediction.entity.PredictionFeeSheetUM
import com.tangem.features.polymarket.impl.placeprediction.entity.PreparedPrediction
import com.tangem.features.polymarket.impl.placeprediction.entity.SellPositionUM
import com.tangem.features.polymarket.impl.placeprediction.entity.SignableQuoteUM
import com.tangem.features.polymarket.impl.placeprediction.entity.SubmitUM
import com.tangem.features.polymarket.impl.placeprediction.entity.canShowFeeBreakdown
import com.tangem.features.polymarket.impl.placeprediction.entity.feeBreakdownQuote
import com.tangem.features.polymarket.impl.placeprediction.model.converter.PlaceOutcome
import com.tangem.features.polymarket.impl.placeprediction.model.converter.PlaceResultUMConverter
import com.tangem.features.polymarket.impl.placeprediction.model.converter.PredictionFeeSheetUMConverter
import com.tangem.features.polymarket.impl.placeprediction.model.transformers.SetBalanceTransformer
import com.tangem.features.polymarket.impl.placeprediction.model.transformers.SetPositionTransformer
import com.tangem.features.polymarket.impl.placeprediction.model.transformers.SetSellPositionStateTransformer
import com.tangem.features.polymarket.impl.placeprediction.model.transformers.SetSubmitTransformer
import com.tangem.utils.coroutines.CoroutineDispatcherProvider
import com.tangem.utils.logging.TangemLogger
import com.tangem.utils.transformer.update
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.filter
import kotlinx.coroutines.flow.launchIn
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.onEach
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import java.math.BigDecimal
import javax.inject.Inject

@Suppress("LongParameterList", "LargeClass")
@ModelScoped
internal class PlacePredictionSummaryModel @Inject constructor(
    paramsContainer: ParamsContainer,
    private val router: Router,
    private val urlOpener: UrlOpener,
    private val getWalletMetaInfoUseCase: GetWalletMetaInfoUseCase,
    private val sendFeedbackEmailUseCase: SendFeedbackEmailUseCase,
    private val messageSender: UiMessageSender,
    override val dispatchers: CoroutineDispatcherProvider,
    private val derivePolymarketAddressesUseCase: DerivePolymarketAddressesUseCase,
    private val getPolymarketBalanceInteractor: GetPolymarketBalanceInteractor,
    private val signPredictionOrderInteractor: SignPredictionOrderInteractor,
    private val placePredictionOrderUseCase: PlacePredictionOrderUseCase,
    private val awaitDelayedPredictionOrderUseCase: AwaitDelayedPredictionOrderUseCase,
    private val positionsRepository: PolymarketPositionsRepository,
    private val predictionFundingWindow: PredictionFundingWindow,
) : Model(), PlacePredictionSummaryIntents {

    private val order = paramsContainer.require<PreparedPrediction>()
    private val params = order.params

    val uiState: StateFlow<PlacePredictionUM>
        field = MutableStateFlow(order.state)

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
        if (uiState.value.submit !is SubmitUM.Idle || !uiState.value.isPrimaryButtonEnabled) return

        uiState.update(SetSubmitTransformer(submit = SubmitUM.Signing))
        modelScope.launch { place(frozen = order.quote) }
    }

    override fun onQuoteRetryClick() {
        if (!isSubmitting()) router.popTo(PlacePredictionRoute.Amount)
    }

    private suspend fun place(frozen: SignableQuoteUM) {
        val isNegRisk = order.isNegRisk
        val addresses = withContext(dispatchers.default) {
            derivePolymarketAddressesUseCase.stored(userWalletId = params.userWalletId)
        }
        if (addresses == null) {
            showPlaceFailure(reason = "No stored addresses")
            return
        }

        when (val power = spendingPower(addresses = addresses, frozen = frozen)) {
            is SpendingPower.Sufficient -> Unit
            is SpendingPower.Insufficient -> {
                returnToIdle()
                return
            }
            is SpendingPower.Refused -> {
                showPlaceFailure(reason = power.reason)
                return
            }
            is SpendingPower.Unknown -> {
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
                    side = params.side,
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

    private suspend fun spendingPower(addresses: PolymarketAddresses, frozen: SignableQuoteUM): SpendingPower =
        when (val params = params) {
            is PlacePredictionComponent.Params.Buy -> collateralPower(
                addresses = addresses,
                required = frozen.quote.total,
            )
            is PlacePredictionComponent.Params.Sell -> sharesPower(
                addresses = addresses,
                positionId = params.positionId,
                required = frozen.quote.shares,
            )
        }

    private suspend fun collateralPower(addresses: PolymarketAddresses, required: BigDecimal): SpendingPower {
        val balance = withContext(dispatchers.default) {
            getPolymarketBalanceInteractor(addresses = addresses).getOrNull()
        } ?: return SpendingPower.Unknown

        uiState.update(SetBalanceTransformer(balance = balance.balance))

        return if (balance.balance >= required) SpendingPower.Sufficient else SpendingPower.Insufficient
    }

    private suspend fun sharesPower(
        addresses: PolymarketAddresses,
        positionId: PolymarketPositionId,
        required: BigDecimal,
    ): SpendingPower {
        val position = withContext(dispatchers.default) {
            positionsRepository.getPosition(addresses = addresses, id = positionId)
        }.fold(
            ifLeft = { return SpendingPower.Unknown },
            ifRight = { position ->
                position ?: run {
                    uiState.update(SetSellPositionStateTransformer(position = SellPositionUM.NotHeld))
                    return SpendingPower.Refused(reason = "Position is no longer held")
                }
            },
        )

        uiState.update(SetPositionTransformer(position = position))

        if (!position.isAcceptingOrders) {
            return SpendingPower.Refused(reason = "Market stopped accepting orders before signing")
        }

        val available = SharesCalculator.available(held = position.size)
        return if (available >= required) {
            SpendingPower.Sufficient
        } else {
            SpendingPower.Refused(reason = "Position holds $available shares, the quote was for $required")
        }
    }

    private sealed interface SpendingPower {

        data object Sufficient : SpendingPower

        data object Insufficient : SpendingPower

        /** @property reason what the exported log says when the order is turned away before a signature */
        data class Refused(val reason: String) : SpendingPower

        data object Unknown : SpendingPower
    }

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
        if (result.movesBalance()) predictionFundingWindow.open(userWalletId = params.userWalletId)

        uiState.update(SetSubmitTransformer(submit = SubmitUM.Result(result = result)))
        router.replaceAll(
            PlacePredictionRoute.Status(
                result = result,
                market = order.state.market,
                side = params.side,
                userWalletId = params.userWalletId,
                positionId = order.positionId,
            ),
        )
    }

    private fun PlaceResultUM.movesBalance(): Boolean = when (this) {
        is PlaceResultUM.Filled,
        is PlaceResultUM.PartiallyFilled,
        is PlaceResultUM.Submitted,
        -> true
        is PlaceResultUM.NotFilled -> false
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
    }

    private fun onSupportClick() {
        modelScope.launch {
            val metaInfo = getWalletMetaInfoUseCase(userWalletId = params.userWalletId).getOrNull() ?: return@launch

            sendFeedbackEmailUseCase(type = FeedbackEmailType.DirectUserRequest(walletMetaInfo = metaInfo))
        }
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

    fun isSubmitting(): Boolean = when (uiState.value.submit) {
        is SubmitUM.Signing,
        is SubmitUM.Posting,
        is SubmitUM.AwaitingFill,
        -> true
        is SubmitUM.Idle,
        is SubmitUM.Result,
        -> false
    }

    private companion object {
        val logger = TangemLogger.withTag(tag = "PlacePredictionSummaryModel")
    }
}