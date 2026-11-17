package com.tangem.common.ui.alerts

import com.tangem.common.ui.R
import com.tangem.core.ui.extensions.TextReference
import com.tangem.core.ui.extensions.resourceReference
import com.tangem.core.ui.extensions.wrappedList
import com.tangem.core.ui.message.DialogMessage
import com.tangem.core.ui.message.EventMessageAction
import com.tangem.domain.transaction.error.SendTransactionError
import javax.inject.Inject

class TransactionErrorDialogFactory @Inject constructor() {

    fun create(
        error: SendTransactionError,
        popBackStack: () -> Unit,
        onFailedTxEmailClick: (String) -> Unit,
        onCancel: () -> Unit = {},
    ): DialogMessage? {
        return when (error) {
            is SendTransactionError.DemoCardError -> demoModeDialog(popBackStack)
            is SendTransactionError.TangemSdkError -> transactionErrorDialog(
                causeTextReference = resourceReference(error.messageRes, wrappedList(error.args)),
                code = error.code.toString(),
                onConfirmClick = { onFailedTxEmailClick(error.code.toString()) },
                onCancel = onCancel,
            )
            is SendTransactionError.BlockchainSdkError -> transactionErrorDialog(
                cause = error.message,
                code = error.code.toString(),
                onConfirmClick = { onFailedTxEmailClick("${error.code}: ${error.message.orEmpty()}") },
                onCancel = onCancel,
            )
            is SendTransactionError.DataError -> transactionErrorDialog(
                cause = error.message,
                code = "",
                onConfirmClick = { onFailedTxEmailClick(error.message.orEmpty()) },
                onCancel = onCancel,
            )
            is SendTransactionError.NetworkError -> transactionErrorDialog(
                cause = error.message.orEmpty(),
                code = error.code.orEmpty(),
                onConfirmClick = { onFailedTxEmailClick(error.message.orEmpty()) },
                onCancel = onCancel,
            )
            is SendTransactionError.UnknownError -> transactionErrorDialog(
                cause = error.ex?.localizedMessage,
                code = "",
                onConfirmClick = { onFailedTxEmailClick(error.ex?.localizedMessage.orEmpty()) },
                onCancel = onCancel,
            )
            is SendTransactionError.CreateAccountUnderfunded -> transactionErrorDialog(
                causeTextReference = resourceReference(
                    R.string.no_account_polkadot,
                    wrappedList(error.amount),
                ),
                code = "",
                onConfirmClick = popBackStack,
            )
            is SendTransactionError.GaslessFeeIncreased -> gaslessFeeIncreasedDialog()
            else -> null
        }
    }

    private fun gaslessFeeIncreasedDialog(): DialogMessage = DialogMessage(
        message = resourceReference(id = R.string.send_notification_high_fee_title),
        firstAction = EventMessageAction(
            title = resourceReference(id = R.string.common_understand),
            onClick = {},
        ),
    )

    private fun demoModeDialog(onConfirmClick: () -> Unit): DialogMessage = DialogMessage(
        title = resourceReference(id = R.string.warning_demo_mode_title),
        message = resourceReference(id = R.string.warning_demo_mode_message),
        firstAction = EventMessageAction(
            title = resourceReference(id = R.string.common_ok),
            onClick = onConfirmClick,
        ),
    )

    private fun transactionErrorDialog(
        cause: String? = null,
        causeTextReference: TextReference? = null,
        code: String,
        onConfirmClick: () -> Unit,
        onCancel: () -> Unit = {},
    ): DialogMessage = DialogMessage(
        title = resourceReference(id = R.string.send_alert_transaction_failed_title),
        message = resourceReference(
            id = R.string.send_alert_transaction_failed_text,
            formatArgs = wrappedList(causeTextReference ?: cause.orEmpty(), code),
        ),
        firstActionBuilder = {
            EventMessageAction(
                title = resourceReference(id = R.string.common_support),
                onClick = onConfirmClick,
            )
        },
        secondActionBuilder = { cancelAction { onCancel() } },
    )
}