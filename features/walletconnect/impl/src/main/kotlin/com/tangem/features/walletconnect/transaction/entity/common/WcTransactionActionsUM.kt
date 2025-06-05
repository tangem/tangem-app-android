package com.tangem.features.walletconnect.transaction.entity.common

import androidx.compose.runtime.Immutable

@Immutable
internal data class WcTransactionActionsUM(
    val onShowVerifiedAlert: (String) -> Unit,
    val onDismiss: () -> Unit,
    val onSign: () -> Unit,
    val onCopy: () -> Unit,
)