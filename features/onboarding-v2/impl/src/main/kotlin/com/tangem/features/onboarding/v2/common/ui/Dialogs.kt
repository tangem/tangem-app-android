package com.tangem.features.onboarding.v2.common.ui

import com.tangem.core.ui.extensions.resourceReference
import com.tangem.core.ui.message.DialogMessage
import com.tangem.core.ui.message.EventMessageAction
import com.tangem.features.onboarding.v2.impl.R

internal fun exitOnboardingDialog(onConfirm: () -> Unit): DialogMessage {
    return DialogMessage(
        title = resourceReference(R.string.onboarding_exit_alert_title),
        message = resourceReference(R.string.onboarding_exit_alert_message),
        firstActionBuilder = {
            EventMessageAction(
                title = resourceReference(R.string.common_yes),
                onClick = onConfirm,
            )
        },
        secondActionBuilder = {
            EventMessageAction(
                title = resourceReference(R.string.common_cancel),
                onClick = {},
            )
        },
    )
}

internal fun interruptBackupDialog(onConfirm: () -> Unit) = DialogMessage(
    title = resourceReference(R.string.onboarding_exit_alert_title),
    message = resourceReference(R.string.onboarding_exit_alert_message),
    firstAction = EventMessageAction(
        title = resourceReference(R.string.common_ok),
        warning = true,
        onClick = onConfirm,
    ),
    secondAction = EventMessageAction(
        title = resourceReference(R.string.common_cancel),
        onClick = {},
    ),
)