package com.tangem.features.send.subcomponents.destination

import com.tangem.core.decompose.di.ModelScoped
import com.tangem.core.decompose.ui.UiMessageSender
import com.tangem.core.ui.extensions.resourceReference
import com.tangem.core.ui.message.DialogMessage
import com.tangem.core.ui.message.EventMessageAction
import com.tangem.features.send.impl.R
import javax.inject.Inject

@ModelScoped
internal class SendDestinationAlertFactory @Inject constructor(
    val messageSender: UiMessageSender,
) {

    fun getGenericErrorState(onFailedTxEmailClick: () -> Unit) {
        messageSender.send(
            DialogMessage(
                title = resourceReference(id = R.string.send_alert_transaction_failed_title),
                message = resourceReference(id = R.string.common_unknown_error),
                firstAction = EventMessageAction(
                    title = resourceReference(R.string.common_support),
                    onClick = onFailedTxEmailClick,
                ),
            ),
        )
    }

    fun showRecipientBackupErrorAlert(onContactSupport: () -> Unit) {
        messageSender.send(
            DialogMessage(
                title = resourceReference(id = R.string.warning_backup_error_add_funds_title),
                message = resourceReference(id = R.string.warning_backup_error_add_funds_message),
                firstAction = EventMessageAction(
                    title = resourceReference(R.string.common_contact_support),
                    onClick = onContactSupport,
                ),
            ),
        )
    }
}