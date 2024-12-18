package com.tangem.tap.features.details.ui.walletconnect

import kotlinx.collections.immutable.ImmutableList

internal data class WalletConnectScreenState(
    val sessions: ImmutableList<WcSessionForScreen>,
    val isLoading: Boolean = false,
    val onRemoveSession: (String) -> Unit = {},
    val onAddSession: () -> Unit = {},
)

data class WcSessionForScreen(
    val description: String,
    val sessionId: String,
)