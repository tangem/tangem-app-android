package com.tangem.features.walletconnect.transaction.entity.common

import com.tangem.features.walletconnect.connections.entity.VerifiedDAppState

internal data class WcTransactionAppInfoContentUM(
    val appName: String,
    val appIcon: String,
    val verifiedState: VerifiedDAppState,
    val appSubtitle: String,
)