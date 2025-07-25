package com.tangem.features.swap.v2.impl.choosetoken.fromSupported.model

import com.tangem.core.decompose.di.ModelScoped
import com.tangem.core.decompose.ui.UiMessageSender
import com.tangem.core.ui.extensions.resourceReference
import com.tangem.core.ui.extensions.stringReference
import com.tangem.core.ui.message.DialogMessage
import com.tangem.core.ui.message.EventMessageAction
import com.tangem.features.swap.v2.impl.R
import javax.inject.Inject

@ModelScoped
internal class SwapChooseTokenAlertFactory @Inject constructor(
    private val uiMessageSender: UiMessageSender,
) {

    fun getGenericErrorState(onDismiss: () -> Unit) {
        uiMessageSender.send(
            DialogMessage(
                title = resourceReference(id = R.string.common_error),
                message = resourceReference(id = R.string.common_unknown_error),
                onDismissRequest = onDismiss,
                firstActionBuilder = { okAction() },
            ),
        )
    }

    fun showChangeTokenAlert(onConfirm: () -> Unit, onDismiss: () -> Unit) {
        // todo fix localization [REDACTED_TASK_KEY]
        uiMessageSender.send(
            DialogMessage(
                title = stringReference("Changing token"),
                message = stringReference(
                    "Are you sure you want to change the token? After changing, previous data will be reset.",
                ),
                firstActionBuilder = {
                    EventMessageAction(
                        title = stringReference("Change"),
                        onClick = onConfirm,
                    )
                },
                secondActionBuilder = {
                    EventMessageAction(
                        title = stringReference("Cancel"),
                        onClick = onDismiss,
                    )
                },
                dismissOnFirstAction = true,
            ),
        )
    }
}