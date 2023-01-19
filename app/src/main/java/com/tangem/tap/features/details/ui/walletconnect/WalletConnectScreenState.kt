package com.tangem.tap.features.details.ui.walletconnect

import com.tangem.tap.features.details.redux.walletconnect.WalletConnectSession

data class WalletConnectScreenState(
    val sessions: List<WcSessionForScreen>,
    val isLoading: Boolean = false,
    val onRemoveSession: (String) -> Unit = {},
    val onAddSession: (String?) -> Unit = {},
)

data class WcSessionForScreen(
    val description: String,
    val sessionId: String,
) {
    companion object {
        fun fromSession(session: WalletConnectSession): WcSessionForScreen {
            return WcSessionForScreen(
                description = session.peerMeta.name,
                sessionId = session.session.toUri(),
            )
        }
    }
}
