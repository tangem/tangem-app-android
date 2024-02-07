package com.tangem.features.send.impl.presentation.state

import com.tangem.blockchain.common.transaction.TransactionFee
import com.tangem.core.ui.event.consumedEvent
import com.tangem.core.ui.event.triggeredEvent
import com.tangem.domain.transaction.error.SendTransactionError
import com.tangem.features.send.impl.presentation.state.fee.FeeSelectorState
import com.tangem.features.send.impl.presentation.state.fee.FeeStateFactory
import com.tangem.features.send.impl.presentation.state.fee.FeeType
import com.tangem.features.send.impl.presentation.viewmodel.SendClickIntents
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
    private val currentStateProvider: Provider<SendUiState>,
    private val clickIntents: SendClickIntents,
    private val feeStateFactory: FeeStateFactory,
) {
    private val sendTransactionErrorConverter by lazy(LazyThreadSafetyMode.NONE) {
        SendTransactionAlertConverter(clickIntents)
    }

    fun onConsumeEventState(): SendUiState {
        return currentStateProvider().copy(event = consumedEvent())
    }

    fun getSendTransactionErrorState(error: SendTransactionError?, onConsume: () -> Unit): SendUiState {
        val state = currentStateProvider()
        val event = error?.let {
            sendTransactionErrorConverter.convert(error)?.let {
                triggeredEvent<SendEvent>(SendEvent.ShowAlert(it), onConsume)
            }
        }
        return state.copy(
            event = event ?: consumedEvent(),
        )
    }

    fun getFeeUpdatedAlert(fee: TransactionFee, onConsume: () -> Unit, onFeeNotIncreased: () -> Unit): SendUiState {
        val state = currentStateProvider()
        val feeSelector = state.feeState?.feeSelectorState as? FeeSelectorState.Content ?: return state
        val newFee = when (fee) {
            is TransactionFee.Single -> fee.normal
            is TransactionFee.Choosable -> {
                when (feeSelector.selectedFee) {
                    FeeType.SLOW -> fee.minimum
                    FeeType.MARKET -> fee.normal
                    FeeType.FAST -> fee.priority
                    FeeType.CUSTOM -> return state
                }
            }
        }

        val newFeeValue = newFee.amount.value ?: BigDecimal.ZERO
        val oldFeeValue = feeStateFactory.feeConverter.convert(feeSelector).amount.value ?: BigDecimal.ZERO
        val updateFeeState = feeStateFactory.onFeeOnLoadedState(fee)
        return if (newFeeValue > oldFeeValue) {
            updateFeeState.copy(
                event = triggeredEvent(
                    data = SendEvent.ShowAlert(SendAlertState.FeeIncreased),
                    onConsume = onConsume,
                ),
            )
        } else {
            onFeeNotIncreased()
            updateFeeState
        }
    }

    fun getGenericErrorState(error: Throwable? = null, onConsume: () -> Unit): SendUiState {
        val state = currentStateProvider()
        return state.copy(
            event = triggeredEvent(
                data = SendEvent.ShowAlert(
                    SendAlertState.GenericError(
                        onConfirmClick = { clickIntents.onFailedTxEmailClick(error?.localizedMessage.orEmpty()) },
                    ),
                ),
                onConsume = onConsume,
            ),
        )
    }
}