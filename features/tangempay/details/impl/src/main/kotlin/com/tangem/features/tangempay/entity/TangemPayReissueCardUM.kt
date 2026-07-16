package com.tangem.features.tangempay.entity

import androidx.compose.runtime.Immutable

@Immutable
internal data class TangemPayReissueCardUM(
    val cardBalance: String,
    val feeAmount: String,
    val isFeeLoading: Boolean,
    val isReissuingInProgress: Boolean,
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
            isFeeLoading: Boolean = false,
            error: TangemPayReissueCardError? = null,
            isReissuingInProgress: Boolean = false,
        ) = TangemPayReissueCardUM(
            feeAmount = feeAmount,
            cardBalance = cardBalance,
            isFeeLoading = isFeeLoading,
            error = error,
            isReissuingInProgress = isReissuingInProgress,
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