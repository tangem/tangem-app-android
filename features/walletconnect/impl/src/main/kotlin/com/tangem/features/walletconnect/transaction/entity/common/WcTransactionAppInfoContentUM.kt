package com.tangem.features.walletconnect.transaction.entity.common

internal data class WcTransactionAppInfoContentUM(
    val appName: String,
    val appIcon: String,
    val isVerified: Boolean,
    val appSubtitle: String,
)