package com.tangem.features.swap.v2.impl.choosetoken.fromSupported.model

import com.tangem.core.decompose.di.ModelScoped
import com.tangem.core.decompose.ui.UiMessageSender
import com.tangem.core.ui.extensions.resourceReference
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
        uiMessageSender.send(
            DialogMessage(
                title = resourceReference(R.string.send_with_swap_change_token_alert_title),
                message = resourceReference(R.string.send_with_swap_change_token_alert_message),
                firstActionBuilder = {
                    EventMessageAction(
                        title = resourceReference(R.string.common_change),
                        onClick = onConfirm,
                    )
                },
                secondActionBuilder = {
                    EventMessageAction(
                        title = resourceReference(R.string.common_cancel),
                        onClick = onDismiss,
                    )
                },
                dismissOnFirstAction = true,
            ),
        )
    }
}