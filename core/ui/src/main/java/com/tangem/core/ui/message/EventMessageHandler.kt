package com.tangem.core.ui.message

import androidx.compose.runtime.Stable
import com.tangem.core.decompose.ui.UiMessage
import com.tangem.core.decompose.ui.UiMessageHandler
import com.tangem.core.ui.event.StateEvent
import com.tangem.core.ui.event.consumedEvent
import com.tangem.core.ui.event.triggeredEvent
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.update

/**
 * Message handler that is used to show or remove an [EventMessage] in the UI.
 */
@Stable
class EventMessageHandler(
    private val events: MutableStateFlow<StateEvent<EventMessage>> = MutableStateFlow(consumedEvent()),
) : UiMessageHandler, StateFlow<StateEvent<EventMessage>> by events {

    override fun handleMessage(message: UiMessage) {
        if (message !is EventMessage) return

        events.value = triggeredEvent(data = message, onConsume = { consumeEvent(message) })
    }

    /**
     * Consumes the [message] only if it is still the triggered one.
     *
     * A message may be replaced by a newer one before it is handled, e.g. when a snackbar is replaced by another
     * snackbar while being displayed. In that case handling of the replaced message is cancelled and it must not reset
     * the state, otherwise the newer message is consumed before it is handled and thus never shown.
     */
    private fun consumeEvent(message: EventMessage) {
        events.update { event ->
            if (event is StateEvent.Triggered && event.data === message) consumedEvent() else event
        }
    }
}