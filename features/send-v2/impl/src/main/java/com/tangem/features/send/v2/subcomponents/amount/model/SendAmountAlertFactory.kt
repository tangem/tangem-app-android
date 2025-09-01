package com.tangem.features.send.v2.subcomponents.amount.model

import com.tangem.core.decompose.di.ModelScoped
import com.tangem.core.decompose.ui.UiMessageSender
import com.tangem.core.ui.extensions.resourceReference
import com.tangem.core.ui.message.DialogMessage
import com.tangem.core.ui.message.EventMessageAction
import com.tangem.features.send.v2.impl.R
import javax.inject.Inject

@ModelScoped
internal class SendAmountAlertFactory @Inject constructor(
    private val uiMessageSender: UiMessageSender,
) {

    fun showResetSendingAlert(onConfirm: () -> Unit) {
        uiMessageSender.send(
            DialogMessage(
                title = resourceReference(R.string.send_with_swap_convert_token_alert_title),
                message = resourceReference(R.string.send_with_swap_convert_token_alert_message),
                firstActionBuilder = {
                    EventMessageAction(
                        title = resourceReference(R.string.common_confirm),
                        onClick = onConfirm,
                    )
                },
                secondActionBuilder = {
                    EventMessageAction(
                        title = resourceReference(R.string.common_not_now),
                        onClick = onDismissRequest,
                    )
                },
                dismissOnFirstAction = true,
            ),
        )
    }
}