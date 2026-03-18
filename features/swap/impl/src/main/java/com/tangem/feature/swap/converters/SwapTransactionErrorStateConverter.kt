package com.tangem.feature.swap.converters

import com.tangem.common.ui.alerts.TransactionErrorDialogFactory
import com.tangem.core.ui.message.DialogMessage
import com.tangem.domain.transaction.error.SendTransactionError
import com.tangem.feature.swap.domain.models.ui.SwapTransactionState
import com.tangem.feature.swap.models.SwapAlertUM
import com.tangem.feature.swap.utils.getExpressErrorMessage
import com.tangem.utils.converter.Converter

internal class SwapTransactionErrorStateConverter(
    private val onDismiss: () -> Unit,
    private val onSupportClick: (String) -> Unit,
    private val transactionErrorDialogFactory: TransactionErrorDialogFactory = TransactionErrorDialogFactory(),
) : Converter<SwapTransactionState.Error, DialogMessage?> {
    override fun convert(value: SwapTransactionState.Error): DialogMessage? {
        return when (value) {
            is SwapTransactionState.Error.TransactionError -> {
                when (val error = value.error) {
                    is SendTransactionError.UserCancelledError -> return null
                    null -> SwapAlertUM.genericError(onDismiss)
                    else -> transactionErrorDialogFactory.create(error, onDismiss, onSupportClick)
                }
            }
            is SwapTransactionState.Error.ExpressError -> {
                SwapAlertUM.expressErrorAlert(
                    message = getExpressErrorMessage(value.error),
                    onConfirmClick = { onSupportClick(value.error.code.toString()) },
                )
            }
            SwapTransactionState.Error.UnknownError -> SwapAlertUM.genericError(onDismiss)
            is SwapTransactionState.Error.TangemPayWithdrawalError -> SwapAlertUM.genericError(
                onConfirmClick = { onSupportClick(value.txId) },
            )
        }
    }
}