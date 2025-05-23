package com.tangem.features.walletconnect.transaction.entity.blockaid

internal data class TransactionCheckResultsUM(
    val estimatedWalletChanges: WcEstimatedWalletChangesUM,
    val notificationText: String? = null,
)