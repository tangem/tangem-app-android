package com.tangem.tap.features.details.redux.walletconnect

import org.rekotlin.Action

object WalletConnectReducer {
    fun reduce(
        action: Action,
        state: WalletConnectState,
    ): WalletConnectState {
        if (action !is WalletConnectAction) return state

        return when (action) {
            is WalletConnectAction.ResetState -> return WalletConnectState()
            is WalletConnectAction.ApproveSession.Success -> {
                state.copy(
                    loading = false,
                    sessions = state.sessions + action.session,
                )
            }
            is WalletConnectAction.OpenSession -> {
                state.copy(loading = true)
            }
            is WalletConnectAction.SetNewSessionData -> {
                state.copy(newSessionData = action.newSession)
            }
            is WalletConnectAction.SetSessionsRestored ->
                WalletConnectState(sessions = action.sessions)
            is WalletConnectAction.RemoveSession -> {
                val sessions =
                    state.sessions.filterNot { it.session.toUri() == action.session.toUri() }
                state.copy(sessions = sessions)
            }
            is WalletConnectAction.UnsupportedCard -> state.copy(loading = false)
            is WalletConnectAction.RefuseOpeningSession -> state.copy(loading = false)
            is WalletConnectAction.OpeningSessionTimeout -> state.copy(loading = false)
            is WalletConnectAction.FailureEstablishingSession -> state.copy(loading = false)
            is WalletConnectAction.UpdateBlockchain -> state.copy(
                sessions = state.sessions
                    .filterNot { it.peerId == action.updatedSession.peerId } + action.updatedSession,
            )

            else -> state
        }
    }
}
