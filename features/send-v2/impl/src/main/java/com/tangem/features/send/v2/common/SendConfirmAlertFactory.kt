package com.tangem.features.send.v2.common

import com.tangem.common.ui.alerts.TransactionErrorDialogFactory
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