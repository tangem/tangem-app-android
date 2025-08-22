package com.tangem.features.send.v2.subcomponents.amount.model

import com.tangem.core.decompose.di.ModelScoped
import com.tangem.core.decompose.ui.UiMessageSender
import com.tangem.core.ui.extensions.stringReference
import com.tangem.core.ui.message.DialogMessage
import com.tangem.core.ui.message.EventMessageAction
import javax.inject.Inject

@ModelScoped
internal class SendAmountAlertFactory @Inject constructor(
    private val uiMessageSender: UiMessageSender,
) {

    fun showResetSendingAlert(onConfirm: () -> Unit) {
        // todo fix localization [REDACTED_TASK_KEY]
        uiMessageSender.send(
            DialogMessage(
                title = stringReference("Confirm Convert"),
                message = stringReference("Proceed with conversion? Previous data will be reset."),
                firstActionBuilder = {
                    EventMessageAction(
                        title = stringReference("Confirm"),
                        onClick = onConfirm,
                    )
                },
                secondActionBuilder = {
                    EventMessageAction(
                        title = stringReference("Not Now"),
                        onClick = onDismissRequest,
                    )
                },
                dismissOnFirstAction = true,
            ),
        )
    }
}