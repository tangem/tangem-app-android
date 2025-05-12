package com.tangem.features.send.v2.send.confirm.model

import com.tangem.common.ui.alerts.TransactionErrorAlertConverter
import com.tangem.common.ui.alerts.models.AlertDemoModeUM
import com.tangem.core.decompose.di.ModelScoped
import com.tangem.core.decompose.ui.UiMessageSender
import com.tangem.core.ui.extensions.resourceReference
import com.tangem.core.ui.message.DialogMessage
import com.tangem.core.ui.message.EventMessageAction
import com.tangem.domain.transaction.error.SendTransactionError
import com.tangem.features.send.v2.impl.R
import javax.inject.Inject

@ModelScoped
internal class SendConfirmAlertFactory @Inject constructor(
    private val messageSender: UiMessageSender,
) {

    fun getGenericErrorState(onFailedTxEmailClick: () -> Unit, popBack: () -> Unit = {}) {
        messageSender.send(
            DialogMessage(
                title = resourceReference(id = R.string.send_alert_transaction_failed_title),
                message = resourceReference(id = R.string.common_unknown_error),
                onDismissRequest = popBack,
                firstAction = EventMessageAction(
                    title = resourceReference(R.string.common_support),
                    onClick = onFailedTxEmailClick,
                ),
            ),
        )
    }

    fun getSendTransactionErrorState(
        error: SendTransactionError?,
        popBack: () -> Unit,
        onFailedTxEmailClick: (String) -> Unit,
    ) {
        val transactionErrorAlertConverter = TransactionErrorAlertConverter(
            popBackStack = popBack,
            onFailedTxEmailClick = onFailedTxEmailClick,
        )

        val errorAlert = error?.let { transactionErrorAlertConverter.convert(error) } ?: return
        val onConfirmClick = errorAlert.onConfirmClick ?: return

        messageSender.send(
            DialogMessage(
                title = errorAlert.title,
                message = errorAlert.message,
                firstActionBuilder = {
                    EventMessageAction(
                        title = errorAlert.confirmButtonText,
                        onClick = onConfirmClick,
                    )
                },
                secondActionBuilder = if (errorAlert !is AlertDemoModeUM) {
                    { cancelAction() }
                } else {
                    null
                },
            ),
        )
    }
}