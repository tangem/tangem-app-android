package com.tangem.core.ui.message

import androidx.compose.runtime.Stable
import com.tangem.core.decompose.ui.UiMessage
import com.tangem.core.decompose.ui.UiMessageHandler
import com.tangem.core.ui.event.StateEvent
import com.tangem.core.ui.event.consumedEvent
import com.tangem.core.ui.event.triggeredEvent
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow

/**
 * Message handler that is used to show or remove an [EventMessage] in the UI.
 */
@Stable
class EventMessageHandler(
    private val events: MutableStateFlow<StateEvent<EventMessage>> = MutableStateFlow(consumedEvent()),
) : UiMessageHandler, StateFlow<StateEvent<EventMessage>> by events {

    override fun handleMessage(message: UiMessage) {
        if (message !is EventMessage) return

        events.value = triggeredEvent(message, ::consumeEvent)
    }

    private fun consumeEvent() {
        events.value = consumedEvent()
    }
}