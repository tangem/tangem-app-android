package com.tangem.feature.swap.converters

import com.tangem.common.ui.alerts.TransactionErrorAlertConverter
import com.tangem.common.ui.alerts.models.AlertUM
import com.tangem.domain.transaction.error.SendTransactionError
import com.tangem.feature.swap.domain.models.ui.SwapTransactionState
import com.tangem.feature.swap.models.SwapAlertUM
import com.tangem.feature.swap.utils.getExpressErrorMessage
import com.tangem.utils.converter.Converter

internal class SwapTransactionErrorStateConverter(
    private val onDismiss: () -> Unit,
    private val onSupportClick: (String) -> Unit,
) : Converter<SwapTransactionState.Error, AlertUM?> {
    override fun convert(value: SwapTransactionState.Error): AlertUM? {
        return when (value) {
            is SwapTransactionState.Error.TransactionError -> {
                when (val error = value.error) {
                    is SendTransactionError.UserCancelledError -> return null
                    null -> SwapAlertUM.GenericError(onDismiss)
                    else -> TransactionErrorAlertConverter(onDismiss, onSupportClick).convert(error)
                }
            }
            is SwapTransactionState.Error.ExpressError -> {
                SwapAlertUM.ExpressErrorAlert(
                    message = getExpressErrorMessage(value.error),
                    onConfirmClick = { onSupportClick(value.error.code.toString()) },
                )
            }
            SwapTransactionState.Error.UnknownError -> SwapAlertUM.GenericError(onDismiss)
        }
    }
}