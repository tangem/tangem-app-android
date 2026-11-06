package com.tangem.feature.swap.converters

import com.tangem.common.ui.R
import com.tangem.common.ui.alerts.TransactionErrorDialogFactory
import com.tangem.core.ui.extensions.TextReference
import com.tangem.core.ui.extensions.resourceReference
import com.tangem.core.ui.message.DialogMessage
import com.tangem.domain.models.currency.CryptoCurrency
import com.tangem.domain.transaction.error.SendTransactionError
import com.tangem.domain.transaction.error.SolanaGaslessError
import com.tangem.feature.swap.domain.models.ui.SwapTransactionState
import com.tangem.feature.swap.model.toExpressError
import com.tangem.feature.swap.models.SwapAlertUM
import com.tangem.feature.swap.utils.getExpressErrorMessage
import com.tangem.utils.converter.Converter

internal class SwapTransactionErrorStateConverter(
    private val isSwapAttemptAlert: Boolean = false,
    private val providerName: String?,
    private val amountCurrency: CryptoCurrency? = null,
    private val onDismiss: () -> Unit,
    private val onSupportClick: (String) -> Unit,
    private val transactionErrorDialogFactory: TransactionErrorDialogFactory = TransactionErrorDialogFactory(),
) : Converter<SwapTransactionState.Error, DialogMessage?> {
    override fun convert(value: SwapTransactionState.Error): DialogMessage? {
        return when (value) {
            is SwapTransactionState.Error.TransactionError -> {
                when (val error = value.error) {
                    is SendTransactionError.UserCancelledError -> null
                    null -> SwapAlertUM.genericError(onDismiss)
                    else -> transactionErrorDialogFactory.create(error, onDismiss, onSupportClick)
                }
            }
            is SwapTransactionState.Error.ExpressError -> {
                SwapAlertUM.expressErrorAlert(
                    message = getExpressErrorMessage(
                        expressError = value.error.toExpressError(),
                        isSwapAttemptAlert = isSwapAttemptAlert,
                        providerName = providerName,
                        amountCurrency = amountCurrency,
                    ),
                    onConfirmClick = { onSupportClick(value.error.code.toString()) },
                )
            }
            SwapTransactionState.Error.UnknownError -> SwapAlertUM.genericError(onDismiss)
            is SwapTransactionState.Error.TangemPayWithdrawalError -> SwapAlertUM.genericError(
                onConfirmClick = { onSupportClick(value.txId) },
            )
            is SwapTransactionState.Error.SolanaGaslessError -> SwapAlertUM.errorAlert(
                onConfirmClick = onDismiss,
                message = value.error.toMessage(),
            )
        }
    }

    private fun SolanaGaslessError.toMessage(): TextReference = resourceReference(
        when (this) {
            SolanaGaslessError.BlockhashExpired -> R.string.gasless_solana_error_expired
            is SolanaGaslessError.RateLimited -> R.string.gasless_solana_error_rate_limited
            is SolanaGaslessError.MaybeSent -> R.string.gasless_solana_error_maybe_sent
            is SolanaGaslessError.Rejected -> R.string.gasless_solana_error_rejected
            SolanaGaslessError.Unauthorized,
            SolanaGaslessError.Disabled,
            is SolanaGaslessError.ServiceUnavailable,
            is SolanaGaslessError.Unknown,
            -> R.string.gasless_solana_error_unavailable
        },
    )
}