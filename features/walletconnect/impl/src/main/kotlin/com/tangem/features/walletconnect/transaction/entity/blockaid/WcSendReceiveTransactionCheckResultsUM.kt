package com.tangem.features.walletconnect.transaction.entity.blockaid

import com.tangem.core.ui.extensions.TextReference
import com.tangem.features.walletconnect.transaction.entity.approve.WcSpendAllowanceUM

internal data class WcSendReceiveTransactionCheckResultsUM(
    val estimatedWalletChanges: WcEstimatedWalletChangesUM? = null,
    val spendAllowance: WcSpendAllowanceUM? = null,
    val notificationText: TextReference? = null,
    val isLoading: Boolean = true,
)