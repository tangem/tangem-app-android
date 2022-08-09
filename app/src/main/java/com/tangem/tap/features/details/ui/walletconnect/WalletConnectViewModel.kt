package com.tangem.tap.features.details.ui.walletconnect

import com.tangem.tap.common.redux.AppState
import com.tangem.tap.features.details.redux.walletconnect.WalletConnectAction
import com.tangem.tap.features.details.redux.walletconnect.WalletConnectSession
import com.tangem.tap.features.details.redux.walletconnect.WalletConnectState
import org.rekotlin.Store

class WalletConnectViewModel(private val store: Store<AppState>) {
    fun updateState(state: WalletConnectState): WalletConnectScreenState {
        return WalletConnectScreenState(
            state.sessions.map { wcSession -> WcSessionForScreen.fromSession(wcSession) },
            isLoading = state.loading,
            onRemoveSession = { sessionUri -> onRemoveSession(sessionUri, state.sessions) },
            onAddSession = { copiedUri -> store.dispatch(WalletConnectAction.StartWalletConnect(copiedUri)) },
        )
    }

    private fun onRemoveSession(sessionUri: String, sessions: List<WalletConnectSession>) {
        sessions
            .firstOrNull { it.session.toUri() == sessionUri }
            ?.let { baseSession ->
                store.dispatch(WalletConnectAction.DisconnectSession(baseSession.session))
            }
    }
}