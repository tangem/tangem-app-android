package com.tangem.features.walletconnect.transaction.entity.blockaid

import com.tangem.core.ui.extensions.TextReference
import com.tangem.features.walletconnect.transaction.entity.approve.WcSpendAllowanceUM

internal data class WcSendReceiveTransactionCheckResultsUM(
    val estimatedWalletChanges: WcEstimatedWalletChangesUM? = null,
    val spendAllowance: WcSpendAllowanceUM? = null,
    val notification: BlockAidNotificationUM? = null,
    val isLoading: Boolean = true,
)

internal data class BlockAidNotificationUM(
    val type: Type,
    val title: TextReference,
    val text: TextReference? = null,
) {
    internal enum class Type {
        ERROR, WARNING
    }
}