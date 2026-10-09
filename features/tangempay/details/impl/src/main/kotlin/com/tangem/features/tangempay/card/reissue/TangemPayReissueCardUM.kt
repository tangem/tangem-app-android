package com.tangem.features.tangempay.card.reissue

import androidx.compose.runtime.Immutable

@Immutable
internal data class TangemPayReissueCardUM(
    val cardBalance: String,
    val feeAmount: String,
    val isConfirmButtonLoading: Boolean,
    val isConfirmButtonEnabled: Boolean,
    val error: TangemPayReissueCardError?,
    val onConfirmClick: () -> Unit,
    val onRetryFee: () -> Unit,
    val onAddFundsClick: () -> Unit,
    val onDismissRequest: () -> Unit,
) {
    companion object {
        fun stub(
            feeAmount: String = "$4.25",
            cardBalance: String = "$0.05",
            error: TangemPayReissueCardError? = null,
            isConfirmButtonLoading: Boolean = false,
            isConfirmButtonEnabled: Boolean = true,
        ) = TangemPayReissueCardUM(
            feeAmount = feeAmount,
            cardBalance = cardBalance,
            error = error,
            isConfirmButtonLoading = isConfirmButtonLoading,
            isConfirmButtonEnabled = isConfirmButtonEnabled,
            onConfirmClick = {},
            onRetryFee = {},
            onAddFundsClick = {},
            onDismissRequest = {},
        )
    }
}

internal enum class TangemPayReissueCardError {
    InsufficientFunds, InitialDataLoading
}