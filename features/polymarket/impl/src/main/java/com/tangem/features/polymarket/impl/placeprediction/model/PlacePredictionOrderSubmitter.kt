package com.tangem.features.polymarket.impl.placeprediction.model

import arrow.core.getOrElse
import com.tangem.core.decompose.ui.UiMessageSender
import com.tangem.core.ui.R
import com.tangem.core.ui.extensions.resourceReference
import com.tangem.core.ui.message.DialogMessage
import com.tangem.core.ui.message.EventMessageAction
import com.tangem.domain.feedback.GetWalletMetaInfoUseCase
import com.tangem.domain.feedback.SendFeedbackEmailUseCase
import com.tangem.domain.feedback.models.FeedbackEmailType
import com.tangem.domain.polymarket.PolymarketPositionsRepository
import com.tangem.domain.polymarket.flow.PredictionFundingWindow
import com.tangem.domain.polymarket.flow.PredictionPositionsUpdateEmitter
import com.tangem.domain.polymarket.interactor.GetPolymarketBalanceInteractor
import com.tangem.domain.polymarket.interactor.PredictionSignRequest
import com.tangem.domain.polymarket.interactor.PredictionOrderSubmitStep
import com.tangem.domain.polymarket.interactor.PredictionSubmitOrderError
import com.tangem.domain.polymarket.interactor.SubmitPredictionOrderInteractor
import com.tangem.domain.polymarket.model.PolymarketAddresses
import com.tangem.domain.polymarket.model.PolymarketPositionId
import com.tangem.domain.polymarket.model.PolymarketSigningError
import com.tangem.domain.polymarket.model.PredictionOrderFailure
import com.tangem.domain.polymarket.model.PredictionOrderSigningPreference
import com.tangem.domain.polymarket.model.PredictionOrderSigningRoute
import com.tangem.domain.polymarket.model.PredictionPlaceOrderError
import com.tangem.domain.polymarket.usecase.AwaitDelayedPredictionOrderUseCase
import com.tangem.domain.polymarket.usecase.DelayedPredictionOrderOutcome
import com.tangem.domain.polymarket.usecase.DerivePolymarketAddressesUseCase
import com.tangem.features.polymarket.api.PolymarketFeatureToggles
import com.tangem.features.polymarket.impl.placeprediction.PlacePredictionComponent
import com.tangem.features.polymarket.impl.placeprediction.entity.PlaceButtonStyleUM
import com.tangem.features.polymarket.impl.placeprediction.entity.PlacePredictionUM
import com.tangem.features.polymarket.impl.placeprediction.entity.PlaceResultUM
import com.tangem.features.polymarket.impl.placeprediction.entity.PreparedPrediction
import com.tangem.features.polymarket.impl.placeprediction.entity.SellPositionUM
import com.tangem.features.polymarket.impl.placeprediction.entity.SignableQuoteUM
import com.tangem.features.polymarket.impl.placeprediction.entity.SubmitUM
import com.tangem.features.polymarket.impl.placeprediction.entity.isReadyToPlace
import com.tangem.features.polymarket.impl.placeprediction.model.converter.PlaceOutcome
import com.tangem.features.polymarket.impl.placeprediction.model.converter.PlaceResultUMConverter
import com.tangem.features.polymarket.impl.placeprediction.model.transformers.SetBalanceTransformer
import com.tangem.features.polymarket.impl.placeprediction.model.transformers.SetPositionTransformer
import com.tangem.features.polymarket.impl.placeprediction.model.transformers.SetSellPositionStateTransformer
import com.tangem.features.polymarket.impl.placeprediction.model.transformers.SetSubmitTransformer
import com.tangem.utils.coroutines.CoroutineDispatcherProvider
import com.tangem.utils.logging.TangemLogger
import com.tangem.utils.transformer.update
import dagger.assisted.Assisted
import dagger.assisted.AssistedFactory
import dagger.assisted.AssistedInject
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import java.math.BigDecimal

@Suppress("LongParameterList", "TooManyFunctions")
internal class PlacePredictionOrderSubmitter @AssistedInject constructor(
    @Assisted private val params: PlacePredictionComponent.Params,
    @Assisted private val uiState: MutableStateFlow<PlacePredictionUM>,
    @Assisted private val scope: CoroutineScope,
    @Assisted private val closeOrder: () -> Unit,
    private val getWalletMetaInfoUseCase: GetWalletMetaInfoUseCase,
    private val sendFeedbackEmailUseCase: SendFeedbackEmailUseCase,
    private val messageSender: UiMessageSender,
    private val dispatchers: CoroutineDispatcherProvider,
    private val derivePolymarketAddressesUseCase: DerivePolymarketAddressesUseCase,
    private val getPolymarketBalanceInteractor: GetPolymarketBalanceInteractor,
    private val submitPredictionOrderInteractor: SubmitPredictionOrderInteractor,
    private val featureToggles: PolymarketFeatureToggles,
    private val awaitDelayedPredictionOrderUseCase: AwaitDelayedPredictionOrderUseCase,
    private val positionsRepository: PolymarketPositionsRepository,
    private val predictionFundingWindow: PredictionFundingWindow,
    private val positionsUpdateEmitter: PredictionPositionsUpdateEmitter,
) {

    private var isAwaitingResultDate = false

    /**
     * @param confirmedWith the button the user confirmed the order on; a plain tap allows the session key only
     * @param onKeyLost told when an order confirmed for the session key cannot be signed with it
     */
    suspend fun submit(
        prepared: PreparedPrediction,
        confirmedWith: PlaceButtonStyleUM,
        onKeyLost: () -> Unit,
    ): PlaceResultUM? {
        if (uiState.value.submit !is SubmitUM.Idle || !uiState.value.isReadyToPlace) return null
        uiState.update(SetSubmitTransformer(submit = SubmitUM.Signing))
        val preference = confirmedWith.toSigningPreference(featureToggles.isPolymarketSessionKeysEnabled)
            ?: return onSessionKeyLost(onKeyLost)
        val result = place(prepared = prepared, preference = preference, onKeyLost = onKeyLost) ?: return null
        isAwaitingResultDate = true
        prepared.awaitLiveResolutionDateMillis()
        return result
    }

    fun isSubmitting(): Boolean = isAwaitingResultDate || when (uiState.value.submit) {
        is SubmitUM.Signing,
        is SubmitUM.Posting,
        is SubmitUM.AwaitingFill,
        -> true
        is SubmitUM.Idle,
        is SubmitUM.Result,
        -> false
    }

    private suspend fun place(
        prepared: PreparedPrediction,
        preference: PredictionOrderSigningPreference,
        onKeyLost: () -> Unit,
    ): PlaceResultUM? {
        val frozen = prepared.quote
        val addresses = withContext(dispatchers.default) {
            derivePolymarketAddressesUseCase.stored(userWalletId = params.userWalletId)
        }
        if (addresses == null) {
            showPlaceFailure(reason = "No stored addresses")
            return null
        }

        when (val power = spendingPower(addresses = addresses, frozen = frozen)) {
            is SpendingPower.Sufficient -> Unit
            is SpendingPower.Insufficient -> {
                returnToIdle()
                return null
            }
            is SpendingPower.Refused -> {
                showPlaceFailure(reason = power.reason)
                return null
            }
            is SpendingPower.Unknown -> {
                showPlaceFailure(reason = "Spending power could not be read")
                return null
            }
        }

        val submitted = withContext(dispatchers.default) {
            submitPredictionOrderInteractor(
                request = PredictionSignRequest(
                    addresses = addresses,
                    quote = frozen.quote,
                    assetId = frozen.assetId,
                    side = params.side,
                    isNegRisk = prepared.isNegRisk,
                ),
                preference = preference,
                onStep = ::onSubmitStep,
            )
        }.getOrElse { error ->
            return when (error) {
                is PredictionSubmitOrderError.NotSigned -> {
                    onSignFailure(failure = error.failure)
                    null
                }
                is PredictionSubmitOrderError.NotPlaced -> onPlaceError(error = error.error)
                PredictionSubmitOrderError.SessionKeyUnavailable -> onSessionKeyLost(onKeyLost)
            }
        }

        val outcome = PlaceResultUMConverter.convert(
            result = submitted.result,
            side = params.side,
            quotedShares = frozen.quote.shares,
            quotedAmount = frozen.quote.notional,
            placedAt = System.currentTimeMillis(),
        )
        return onPlaceOutcome(
            outcome = outcome,
            addresses = addresses,
            quotedAmount = frozen.quote.notional,
            route = submitted.route,
        )
    }

    private fun onSessionKeyLost(onKeyLost: () -> Unit): PlaceResultUM? {
        onKeyLost()
        showPlaceFailure(reason = "The session key could not sign an order confirmed for it")
        return null
    }

    private fun onSubmitStep(step: PredictionOrderSubmitStep) {
        val submit = when (step) {
            PredictionOrderSubmitStep.SIGNING -> SubmitUM.Signing
            PredictionOrderSubmitStep.POSTING -> SubmitUM.Posting
        }
        uiState.update(SetSubmitTransformer(submit = submit))
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

    private fun onSignFailure(failure: PredictionOrderFailure) {
        val isCancelled = failure is PredictionOrderFailure.Signing &&
            failure.error == PolymarketSigningError.UserCancelled

        if (isCancelled) {
            returnToIdle()
        } else {
            showPlaceFailure(reason = failure.toReason())
        }
    }

    private fun onPlaceError(error: PredictionPlaceOrderError): PlaceResultUM? {
        when (error) {
            is PredictionPlaceOrderError.Ambiguous -> {
                refreshBalanceAndPositions()
                showPlaceFailure(reason = "Order outcome unknown: $error")
            }
            is PredictionPlaceOrderError.NoLiquidity -> {
                logger.e("The exchange found no match for the order: $error")
                return showResult(result = PlaceResultUM.NotFilled(placedAt = System.currentTimeMillis()))
            }
            is PredictionPlaceOrderError.Unauthorized -> {
                logger.e("The exchange refused the credentials, leaving the gate to reconnect: $error")
                returnToIdle()
            }
            else -> showPlaceFailure(reason = "Order refused: $error")
        }
        return null
    }

    private suspend fun onPlaceOutcome(
        outcome: PlaceOutcome,
        addresses: PolymarketAddresses,
        quotedAmount: BigDecimal,
        route: PredictionOrderSigningRoute,
    ): PlaceResultUM? = when (outcome) {
        is PlaceOutcome.Reportable -> showResult(result = outcome.result)
        is PlaceOutcome.Anomalous -> {
            showPlaceFailure(reason = outcome.reason, closesOrder = true)
            null
        }
        is PlaceOutcome.Delayed -> awaitDelayedFill(
            addresses = addresses,
            orderId = outcome.orderId,
            quotedAmount = quotedAmount,
            placedAt = outcome.placedAt,
            route = route,
        )
    }

    /**
     * The exchange accepted the order into a matching delay, so the answer to "did it fill" is read back from
     * the book. A match is returned as a result like an immediate one, and so is an order the book has not
     * settled in time: it may still fill, so the form must not offer to place it again. A rejection is a failure
     * dialog, since the same order can be placed again.
     */
    private suspend fun awaitDelayedFill(
        addresses: PolymarketAddresses,
        orderId: String,
        quotedAmount: BigDecimal,
        placedAt: Long,
        route: PredictionOrderSigningRoute,
    ): PlaceResultUM? {
        uiState.update(SetSubmitTransformer(submit = SubmitUM.AwaitingFill))

        val outcome = withContext(dispatchers.default) {
            awaitDelayedPredictionOrderUseCase(addresses = addresses, orderId = orderId, route = route)
        }

        when (outcome) {
            is DelayedPredictionOrderOutcome.Matched -> return onPlaceOutcome(
                outcome = PlaceResultUMConverter.convert(
                    order = outcome.order,
                    quotedAmount = quotedAmount,
                    placedAt = placedAt,
                ),
                addresses = addresses,
                quotedAmount = quotedAmount,
                route = route,
            )
            is DelayedPredictionOrderOutcome.Failed -> {
                showPlaceFailure(reason = "Delayed order $orderId did not fill: ${outcome.detail}")
            }
            is DelayedPredictionOrderOutcome.Unconfirmed -> {
                logger.e("Delayed order $orderId: the book did not say whether it matched within the reads allowed")
                return showResult(result = PlaceResultUM.StillProcessing(placedAt = placedAt))
            }
        }
        return null
    }

    private fun showResult(result: PlaceResultUM): PlaceResultUM {
        if (result.movesBalance()) {
            refreshBalanceAndPositions()
        }

        uiState.update(SetSubmitTransformer(submit = SubmitUM.Result(result = result)))
        return result
    }

    private fun refreshBalanceAndPositions() {
        predictionFundingWindow.open(userWalletId = params.userWalletId)
        positionsUpdateEmitter.triggerUpdate(userWalletId = params.userWalletId)
    }

    private fun PlaceResultUM.movesBalance(): Boolean = when (this) {
        is PlaceResultUM.Filled,
        is PlaceResultUM.PartiallyFilled,
        is PlaceResultUM.Submitted,
        is PlaceResultUM.StillProcessing,
        -> true
        is PlaceResultUM.NotFilled -> false
    }

    private fun showPlaceFailure(reason: String, closesOrder: Boolean = false) {
        logger.e(reason)
        returnToIdle()

        messageSender.send(
            DialogMessage(
                title = resourceReference(R.string.common_something_went_wrong),
                message = resourceReference(R.string.prediction_place_error_description),
                isDismissable = !closesOrder,
                firstActionBuilder = {
                    EventMessageAction(
                        title = resourceReference(R.string.common_support),
                        onClick = { onSupportClick(thenCloseOrder = closesOrder) },
                    )
                },
                secondActionBuilder = {
                    if (closesOrder) {
                        EventMessageAction(title = resourceReference(R.string.common_close), onClick = closeOrder)
                    } else {
                        cancelAction()
                    }
                },
            ),
        )
    }

    private fun returnToIdle() {
        uiState.update(SetSubmitTransformer(submit = SubmitUM.Idle))
    }

    private fun onSupportClick(thenCloseOrder: Boolean) {
        scope.launch {
            getWalletMetaInfoUseCase(userWalletId = params.userWalletId).getOrNull()?.let { metaInfo ->
                sendFeedbackEmailUseCase(type = FeedbackEmailType.DirectUserRequest(walletMetaInfo = metaInfo))
            }
            if (thenCloseOrder) closeOrder()
        }
    }

    @AssistedFactory
    interface Factory {
        fun create(
            params: PlacePredictionComponent.Params,
            uiState: MutableStateFlow<PlacePredictionUM>,
            scope: CoroutineScope,
            closeOrder: () -> Unit,
        ): PlacePredictionOrderSubmitter
    }

    private companion object {
        val logger = TangemLogger.withTag(tag = "PlacePredictionOrderSubmitter")
    }
}

private fun PlaceButtonStyleUM.toSigningPreference(isSessionKeysEnabled: Boolean): PredictionOrderSigningPreference? =
    when {
        this == PlaceButtonStyleUM.PLAIN && !isSessionKeysEnabled -> null
        this == PlaceButtonStyleUM.PLAIN -> PredictionOrderSigningPreference.SESSION_KEY_ONLY
        isSessionKeysEnabled -> PredictionOrderSigningPreference.AUTOMATIC
        else -> PredictionOrderSigningPreference.OWNER_ONLY
    }

private sealed interface SpendingPower {

    data object Sufficient : SpendingPower

    data object Insufficient : SpendingPower

    /** @property reason what the exported log says when the order is turned away before a signature */
    data class Refused(val reason: String) : SpendingPower

    data object Unknown : SpendingPower
}

private fun PredictionOrderFailure.toReason(): String = when (this) {
    is PredictionOrderFailure.Draft -> "Order could not be drafted: $error"
    is PredictionOrderFailure.Invalid -> "Order failed its own checks: $error"
    is PredictionOrderFailure.Signing -> "Signing failed: $error"
}