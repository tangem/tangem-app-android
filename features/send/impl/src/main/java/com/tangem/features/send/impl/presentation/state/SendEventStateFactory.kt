package com.tangem.features.send.impl.presentation.state

import com.tangem.core.ui.event.consumedEvent
import com.tangem.core.ui.event.triggeredEvent
import com.tangem.domain.transaction.error.SendTransactionError
import com.tangem.features.send.impl.presentation.viewmodel.SendClickIntents
import com.tangem.utils.Provider

/**
 * Factory to produce event state for [SendUiState]
 *
 * @param currentStateProvider [Provider] of [SendUiState]
 * @param clickIntents [SendClickIntents]
 */
internal class SendEventStateFactory(
    val currentStateProvider: Provider<SendUiState>,
    val clickIntents: SendClickIntents,
) {
    private val sendTransactionErrorConverter by lazy { SendTransactionAlertConverter(clickIntents) }

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