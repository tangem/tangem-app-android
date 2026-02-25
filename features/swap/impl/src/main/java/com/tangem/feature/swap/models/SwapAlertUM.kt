package com.tangem.feature.swap.models

import com.tangem.core.ui.R
import com.tangem.core.ui.extensions.TextReference
import com.tangem.core.ui.extensions.resourceReference
import com.tangem.core.ui.message.DialogMessage
import com.tangem.core.ui.message.EventMessageAction

internal object SwapAlertUM {

    fun genericError(
        onConfirmClick: () -> Unit,
        message: TextReference = resourceReference(R.string.common_unknown_error),
    ): DialogMessage = DialogMessage(
        title = null,
        message = message,
        firstAction = EventMessageAction(
            title = resourceReference(id = R.string.common_support),
            onClick = onConfirmClick,
        ),
    )

    fun expressErrorAlert(
        message: TextReference = resourceReference(R.string.common_unknown_error),
        onConfirmClick: () -> Unit,
    ): DialogMessage = DialogMessage(
        title = null,
        message = message,
        firstAction = EventMessageAction(
            title = resourceReference(id = R.string.common_support),
            onClick = onConfirmClick,
        ),
    )

    fun informationAlert(message: TextReference, onConfirmClick: () -> Unit): DialogMessage = DialogMessage(
        title = resourceReference(R.string.swapping_alert_title),
        message = message,
        firstAction = EventMessageAction(
            title = resourceReference(id = R.string.common_ok),
            onClick = onConfirmClick,
        ),
    )
}