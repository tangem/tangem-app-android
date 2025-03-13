package com.tangem.features.send.impl.presentation.state

import com.tangem.blockchain.common.transaction.TransactionFee
import com.tangem.common.ui.alerts.TransactionErrorAlertConverter
import com.tangem.core.ui.event.consumedEvent
import com.tangem.core.ui.event.triggeredEvent
import com.tangem.domain.transaction.error.SendTransactionError
import com.tangem.features.send.impl.presentation.state.fee.FeeSelectorState
import com.tangem.features.send.impl.presentation.state.fee.FeeStateFactory
import com.tangem.features.send.impl.presentation.state.fee.FeeType
import com.tangem.features.send.impl.presentation.model.SendClickIntents
import com.tangem.utils.Provider
import java.math.BigDecimal

/**
 * Factory to produce event state for [SendUiState]
 *
 * @param currentStateProvider [Provider] of [SendUiState]
 * @param clickIntents [SendClickIntents]
 * @param feeStateFactory [FeeStateFactory]
 */
internal class SendEventStateFactory(
    private val stateRouterProvider: Provider<StateRouter>,
    private val currentStateProvider: Provider<SendUiState>,
    private val clickIntents: SendClickIntents,
    private val feeStateFactory: FeeStateFactory,
) {
    private val transactionErrorAlertConverter by lazy(LazyThreadSafetyMode.NONE) {
        TransactionErrorAlertConverter(
            popBackStack = clickIntents::popBackStack,
            onFailedTxEmailClick = clickIntents::onFailedTxEmailClick,
        )
    }

    fun onConsumeEventState(): SendUiState {
        return currentStateProvider().copy(event = consumedEvent())
    }

    fun getSendTransactionErrorState(error: SendTransactionError?, onConsume: () -> Unit): SendUiState {
        val state = currentStateProvider()
        val event = error?.let {
            transactionErrorAlertConverter.convert(error)?.let {
                triggeredEvent<SendEvent>(SendEvent.ShowAlert(it), onConsume)
            }
        }
        return state.copy(
            event = event ?: consumedEvent(),
        )
    }

    fun getFeeUpdatedAlert(fee: TransactionFee, onConsume: () -> Unit, onFeeNotIncreased: () -> Unit): SendUiState {
        val state = currentStateProvider()
        val feeState = state.getFeeState(stateRouterProvider().isEditState)
        val feeSelector = feeState?.feeSelectorState as? FeeSelectorState.Content ?: return state
        val newFee = when (fee) {
            is TransactionFee.Single -> fee.normal
            is TransactionFee.Choosable -> {
                when (feeSelector.selectedFee) {
                    FeeType.Slow -> fee.minimum
                    FeeType.Market -> fee.normal
                    FeeType.Fast -> fee.priority
                    FeeType.Custom -> return state
                }
            }
        }

        val newFeeValue = newFee.amount.value ?: BigDecimal.ZERO
        val oldFeeValue = feeStateFactory.feeConverter.convert(feeSelector).amount.value ?: BigDecimal.ZERO
        val updateFeeState = feeStateFactory.onFeeOnLoadedState(fee)
        return if (newFeeValue > oldFeeValue) {
            updateFeeState.copy(
                event = triggeredEvent(
                    data = SendEvent.ShowAlert(SendAlertUM.FeeIncreased(onConsume)),
                    onConsume = onConsume,
                ),
            )
        } else {
            onFeeNotIncreased()
            updateFeeState
        }
    }

    fun getFeeTooLowAlert(onConsume: () -> Unit): SendUiState {
        val state = currentStateProvider()
        return state.copy(
            event = triggeredEvent(
                data = SendEvent.ShowAlert(
                    SendAlertUM.FeeTooLow(
                        onConfirmClick = clickIntents::showSend,
                    ),
                ),
                onConsume = onConsume,
            ),
        )
    }

    fun getFeeTooHighAlert(diff: String, onConsume: () -> Unit): SendUiState {
        return currentStateProvider().copy(
            event = triggeredEvent(
                data = SendEvent.ShowAlert(
                    SendAlertUM.FeeTooHigh(
                        onConfirmClick = clickIntents::showSend,
                        times = diff,
                    ),
                ),
                onConsume = onConsume,
            ),
        )
    }

    fun getGenericErrorState(error: Throwable? = null, onConsume: () -> Unit): SendUiState {
        val state = currentStateProvider()
        return state.copy(
            event = triggeredEvent(
                data = SendEvent.ShowAlert(
                    SendAlertUM.GenericError(
                        onConfirmClick = { clickIntents.onFailedTxEmailClick(error?.localizedMessage.orEmpty()) },
                    ),
                ),
                onConsume = onConsume,
            ),
        )
    }

    fun getFeeUnreachableErrorState(onConsume: () -> Unit): SendUiState {
        val state = currentStateProvider()
        return state.copy(
            event = triggeredEvent(
                data = SendEvent.ShowAlert(
                    SendAlertUM.FeeUnreachableError(onConfirmClick = clickIntents::feeReload),
                ),
                onConsume = onConsume,
            ),
        )
    }
}