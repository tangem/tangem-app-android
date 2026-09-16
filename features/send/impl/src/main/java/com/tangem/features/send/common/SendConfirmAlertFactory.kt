package com.tangem.features.send.common

import com.tangem.common.ui.alerts.TransactionErrorDialogFactory
import com.tangem.core.decompose.di.ModelScoped
import com.tangem.core.decompose.ui.UiMessageSender
import com.tangem.core.ui.extensions.resourceReference
import com.tangem.core.ui.message.DialogMessage
import com.tangem.core.ui.message.EventMessageAction
import com.tangem.domain.transaction.error.SendTransactionError
import com.tangem.features.send.impl.R
import javax.inject.Inject

@ModelScoped
internal class SendConfirmAlertFactory @Inject constructor(
    private val messageSender: UiMessageSender,
    private val transactionErrorDialogFactory: TransactionErrorDialogFactory,
) {

    fun getGenericErrorState(onFailedTxEmailClick: () -> Unit, popBack: () -> Unit = {}) {
        messageSender.send(
            DialogMessage(
                title = resourceReference(id = R.string.send_alert_transaction_failed_title),
                message = resourceReference(id = R.string.common_unknown_error),
                onDismissRequest = popBack,
                firstActionBuilder = {
                    EventMessageAction(
                        title = resourceReference(R.string.common_support),
                        onClick = onFailedTxEmailClick,
                    )
                },
                secondActionBuilder = { cancelAction { } },
            ),
        )
    }

    fun getJointSlotBusyState() = sendJointNotice(
        title = R.string.joint_account_send_operation_in_progress_title,
        message = R.string.joint_account_send_operation_in_progress_subtitle,
    )

    fun getJointAccountNotReadyState() = sendJointNotice(
        title = R.string.joint_account_send_not_activated_title,
        message = R.string.joint_account_send_not_activated_subtitle,
    )

    fun getJointNetworkStateMovedState() = sendJointNotice(
        title = R.string.joint_account_send_network_changed_title,
        message = R.string.joint_account_send_network_changed_subtitle,
    )

    fun getJointSendBlockedState(onSupportClick: () -> Unit) {
        messageSender.send(
            DialogMessage(
                title = resourceReference(id = R.string.joint_account_send_blocked_title),
                message = resourceReference(id = R.string.joint_account_send_blocked_subtitle),
                firstActionBuilder = {
                    EventMessageAction(title = resourceReference(R.string.common_support), onClick = onSupportClick)
                },
                secondActionBuilder = {
                    EventMessageAction(title = resourceReference(R.string.common_close), onClick = onDismissRequest)
                },
            ),
        )
    }

    private fun sendJointNotice(title: Int, message: Int) {
        messageSender.send(
            DialogMessage(
                title = resourceReference(id = title),
                message = resourceReference(id = message),
                firstActionBuilder = {
                    EventMessageAction(title = resourceReference(R.string.common_got_it), onClick = onDismissRequest)
                },
            ),
        )
    }

    fun getSendTransactionErrorState(
        error: SendTransactionError,
        popBack: () -> Unit,
        onFailedTxEmailClick: (String) -> Unit,
    ) {
        val errorDialog = transactionErrorDialogFactory.create(
            error = error,
            popBackStack = popBack,
            onFailedTxEmailClick = onFailedTxEmailClick,
        ) ?: return

        messageSender.send(errorDialog)
    }
}