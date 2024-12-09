package com.tangem.tap.features.details.redux.walletconnect

import org.rekotlin.Action

object WalletConnectReducer {
    fun reduce(action: Action, state: WalletConnectState): WalletConnectState {
        if (action !is WalletConnectAction) return state

        return when (action) {
            is WalletConnectAction.OpenSession -> {
                state.copy(loading = true)
            }
            is WalletConnectAction.ApproveProposal -> state.copy(loading = true)
            is WalletConnectAction.RejectProposal,
            is WalletConnectAction.SessionEstablished,
            is WalletConnectAction.SessionRejected,
            is WalletConnectAction.PairConnectErrorAction,
            is WalletConnectAction.UnsupportedDappRequest,
            -> state.copy(loading = false)
            is WalletConnectAction.SessionListUpdated -> state.copy(
                wc2Sessions = action.sessions,
            )
            else -> state
        }
    }
}