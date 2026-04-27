package com.tangem.features.walletconnect.pay.entity

import androidx.compose.runtime.Immutable
import com.tangem.core.ui.extensions.TextReference

@Immutable
internal data class WcPayUM(
    val state: State,
    val merchantName: String,
    val merchantIconUrl: String?,
    val paymentAmount: String,
    val paymentCurrency: String,
    val options: List<PaymentOptionUM>,
    val selectedOptionId: String?,
    val onOptionSelected: (String) -> Unit,
    val kycUrl: String?,
    val signingCurrent: Int,
    val signingTotal: Int,
    val onContinue: () -> Unit,
    val onDismiss: () -> Unit,
    val errorMessage: TextReference?,
) {
    enum class State {
        LOADING,
        OPTIONS,
        KYC_WEBVIEW,
        SIGNING,
        SUCCESS,
        FAILED,
    }
}

@Immutable
internal data class PaymentOptionUM(
    val id: String,
    val networkName: String,
    val networkIconUrl: String?,
    val tokenSymbol: String,
    val tokenIconUrl: String?,
    val amount: String,
    val estimatedTxs: Int?,
    val requiresKyc: Boolean,
    val isSelected: Boolean,
)