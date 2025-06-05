package com.tangem.features.walletconnect.transaction.entity.blockaid

import com.tangem.core.ui.extensions.TextReference

internal data class WcSendReceiveTransactionCheckResultsUM(
    val estimatedWalletChanges: WcEstimatedWalletChangesUM?,
    val notificationText: TextReference? = null,
)