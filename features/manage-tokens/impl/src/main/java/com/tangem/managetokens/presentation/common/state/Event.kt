package com.tangem.managetokens.presentation.common.state

import androidx.compose.runtime.Immutable

@Immutable
internal sealed interface Event {
    data class ShowAlert(val state: AlertState) : Event
}