package com.tangem.tap.features.details.redux.walletconnect

import org.rekotlin.Action

class WalletConnectReducer {
    companion object {
        fun reduce(
            action: Action, state: WalletConnectState,
        ): WalletConnectState {
            if (action !is WalletConnectAction) return state

            return when (action) {
                is WalletConnectAction.ApproveSession.Success -> {
                    state.copy(
                        loading = false,
                        sessions = state.sessions + action.session
                    )
                }
                is WalletConnectAction.SetSessionsRestored ->
                    WalletConnectState(sessions = action.sessions)

                is WalletConnectAction.RemoveSession -> {
                    val sessions =
                        state.sessions.filterNot { it.session.toUri() == action.session.toUri() }
                    state.copy(sessions = sessions)
                }
                is WalletConnectAction.ScanCard -> state.copy(loading = true)

                is WalletConnectAction.FailureEstablishingSession -> state.copy(loading = false)

                else -> state
            }
        }

    }
}