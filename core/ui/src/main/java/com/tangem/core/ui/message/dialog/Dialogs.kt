package com.tangem.core.ui.message.dialog

import com.tangem.core.error.UniversalError
import com.tangem.core.ui.R
import com.tangem.core.ui.extensions.TextReference
import com.tangem.core.ui.extensions.resourceReference
import com.tangem.core.ui.extensions.wrappedList
import com.tangem.core.ui.message.DialogMessage
import com.tangem.core.ui.message.EventMessageAction

/**
[REDACTED_AUTHOR]
 */
object Dialogs {

    /**
     * Card verification failed dialog
     *
     * @param errorDescription sdk error description
     * @param onRequestSupport lambda be invoked when request support action is clicked
     * @param onCancelClick    lambda be invoked when cancel action is clicked
     */
    fun cardVerificationFailed(
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

    /**
     * Universal error dialog
     */
    fun universalErrorDialog(universalError: UniversalError, onDismiss: () -> Unit): DialogMessage {
        return DialogMessage(
            message = resourceReference(R.string.universal_error, wrappedList(universalError.errorCode)),
            firstAction = EventMessageAction(
                title = resourceReference(R.string.common_ok),
                onClick = {},
            ),
            dismissOnFirstAction = true,
            onDismissRequest = onDismiss,
        )
    }
}