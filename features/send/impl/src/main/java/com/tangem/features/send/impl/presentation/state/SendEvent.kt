package com.tangem.features.send.impl.presentation.state

import androidx.compose.runtime.Immutable
import com.tangem.common.ui.alerts.models.AlertUM
import com.tangem.core.ui.extensions.TextReference

@Immutable
internal sealed class SendEvent {

    data class ShowSnackBar(val text: TextReference) : SendEvent()

    data class ShowAlert(val alert: AlertUM) : SendEvent()
}