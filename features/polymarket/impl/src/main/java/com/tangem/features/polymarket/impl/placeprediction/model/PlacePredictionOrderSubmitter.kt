package com.tangem.features.polymarket.impl.placeprediction.model

import arrow.core.getOrElse
import com.tangem.core.decompose.ui.UiMessageSender
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
import com.tangem.domain.polymarket.flow.PredictionPositionsUpdateEmitter
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
import com.tangem.features.polymarket.impl.placeprediction.PlacePredictionComponent
import com.tangem.features.polymarket.impl.placeprediction.entity.PlacePredictionUM
import com.tangem.features.polymarket.impl.placeprediction.entity.PlaceResultUM
import com.tangem.features.polymarket.impl.placeprediction.entity.PreparedPrediction
import com.tangem.features.polymarket.impl.placeprediction.entity.SellPositionUM
import com.tangem.features.polymarket.impl.placeprediction.entity.SignableQuoteUM
import com.tangem.features.polymarket.impl.placeprediction.entity.SubmitUM
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
    private val getWalletMetaInfoUseCase: GetWalletMetaInfoUseCase,
    private val sendFeedbackEmailUseCase: SendFeedbackEmailUseCase,
    private val messageSender: UiMessageSender,
    private val dispatchers: CoroutineDispatcherProvider,
    private val derivePolymarketAddressesUseCase: DerivePolymarketAddressesUseCase,
    private val getPolymarketBalanceInteractor: GetPolymarketBalanceInteractor,
    private val signPredictionOrderInteractor: SignPredictionOrderInteractor,
    private val placePredictionOrderUseCase: PlacePredictionOrderUseCase,
    private val awaitDelayedPredictionOrderUseCase: AwaitDelayedPredictionOrderUseCase,
    private val positionsRepository: PolymarketPositionsRepository,
    private val predictionFundingWindow: PredictionFundingWindow,
    private val positionsUpdateEmitter: PredictionPositionsUpdateEmitter,
) {

    suspend fun submit(prepared: PreparedPrediction): PlaceResultUM? {
        uiState.update(SetSubmitTransformer(submit = SubmitUM.Signing))
        return place(frozen = prepared.quote, isNegRisk = prepared.isNegRisk)
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

    private suspend fun place(frozen: SignableQuoteUM, isNegRisk: Boolean): PlaceResultUM? {
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

        val signedOrder = withContext(dispatchers.default) {
            signPredictionOrderInteractor(
                request = PredictionSignRequest(
                    addresses = addresses,
                    quote = frozen.quote,
                    assetId = frozen.assetId,
                    side = params.side,
                    isNegRisk = isNegRisk,
                    quoteReceivedAtMillis = frozen.receivedAtMillis,
                ),
            )
        }.getOrElse { failure ->
            onSignFailure(failure = failure)
            return null
        }

        uiState.update(SetSubmitTransformer(submit = SubmitUM.Posting))

        return withContext(dispatchers.default) {
            placePredictionOrderUseCase(
                addresses = addresses,
                signedOrder = signedOrder,
                version = frozen.quote.version,
            )
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
    ): PlaceResultUM? = when (outcome) {
        is PlaceOutcome.Reportable -> showResult(result = outcome.result)
        is PlaceOutcome.Anomalous -> {
            showPlaceFailure(reason = outcome.reason)
            null
        }
        is PlaceOutcome.Delayed -> awaitDelayedFill(
            addresses = addresses,
            orderId = outcome.orderId,
            quotedAmount = quotedAmount,
            placedAt = outcome.placedAt,
        )
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
    ): PlaceResultUM? {
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
                    return showResult(result = result)
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
        -> true
        is PlaceResultUM.NotFilled -> false
    }

    private fun showPlaceFailure(reason: String) {
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
        scope.launch {
            val metaInfo = getWalletMetaInfoUseCase(userWalletId = params.userWalletId).getOrNull() ?: return@launch

            sendFeedbackEmailUseCase(type = FeedbackEmailType.DirectUserRequest(walletMetaInfo = metaInfo))
        }
    }

    @AssistedFactory
    interface Factory {
        fun create(
            params: PlacePredictionComponent.Params,
            uiState: MutableStateFlow<PlacePredictionUM>,
            scope: CoroutineScope,
        ): PlacePredictionOrderSubmitter
    }

    private companion object {
        val logger = TangemLogger.withTag(tag = "PlacePredictionOrderSubmitter")
    }
}