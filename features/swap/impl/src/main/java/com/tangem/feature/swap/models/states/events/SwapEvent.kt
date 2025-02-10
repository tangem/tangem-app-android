package com.tangem.feature.swap.models.states.events

import androidx.compose.runtime.Immutable
import com.tangem.common.ui.alerts.models.AlertUM

@Immutable
internal sealed class SwapEvent {
    data class ShowAlert(val alert: AlertUM) : SwapEvent()
}