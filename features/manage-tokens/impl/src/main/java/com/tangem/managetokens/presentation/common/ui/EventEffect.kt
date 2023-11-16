package com.tangem.managetokens.presentation.common.ui

import androidx.compose.runtime.Composable
import com.tangem.core.ui.event.StateEvent
import com.tangem.managetokens.presentation.common.state.AlertState
import com.tangem.managetokens.presentation.common.state.Event

@Composable
internal fun EventEffect(event: StateEvent<Event>, onAlertStateSet: (AlertState) -> Unit) {
    com.tangem.core.ui.event.EventEffect(
        event = event,
        onTrigger = { value ->
            when (value) {
                is Event.ShowAlert -> onAlertStateSet(value.state)
            }
        },
    )
}
