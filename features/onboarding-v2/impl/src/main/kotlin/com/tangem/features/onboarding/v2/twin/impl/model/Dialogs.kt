package com.tangem.features.onboarding.v2.twin.impl.model

import com.tangem.core.ui.extensions.TextReference
import com.tangem.core.ui.extensions.resourceReference
import com.tangem.core.ui.message.DialogMessage
import com.tangem.core.ui.message.EventMessageAction
import com.tangem.features.onboarding.v2.impl.R

internal val TwinProcessNotCompleted = DialogMessage(
    message = resourceReference(R.string.onboarding_twin_exit_warning),
    firstAction = EventMessageAction(
        title = resourceReference(R.string.warning_button_ok),
        onClick = {},
    ),
)

internal fun cardVerificationFailedDialog(
    errorDescription: TextReference,
    onRequestSupport: () -> Unit,
    onCancelClick: () -> Unit = {},
): DialogMessage {
    return DialogMessage(
        message = errorDescription,
        title = resourceReference(id = R.string.security_alert_title),
        isDismissable = false,
        firstActionBuilder = {
            EventMessageAction(
                title = resourceReference(id = R.string.alert_button_request_support),
                onClick = onRequestSupport,
            )
        },
        secondActionBuilder = { cancelAction(onClick = onCancelClick) },
    )
}