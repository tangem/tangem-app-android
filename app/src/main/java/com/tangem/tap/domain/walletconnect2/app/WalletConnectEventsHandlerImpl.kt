package com.tangem.tap.domain.walletconnect2.app

import com.tangem.tap.common.extensions.dispatchOnMain
import com.tangem.tap.common.redux.global.GlobalAction
import com.tangem.tap.domain.walletconnect2.domain.WalletConnectEventsHandler
import com.tangem.tap.domain.walletconnect2.domain.WcPreparedRequest
import com.tangem.tap.domain.walletconnect2.domain.models.WalletConnectError
import com.tangem.tap.domain.walletconnect2.domain.models.WalletConnectEvents
import com.tangem.tap.features.details.redux.walletconnect.WalletConnectAction
import com.tangem.tap.features.details.redux.walletconnect.WalletConnectDialog
import com.tangem.tap.features.details.ui.walletconnect.WcSessionForScreen
import com.tangem.tap.store
import timber.log.Timber

internal class WalletConnectEventsHandlerImpl : WalletConnectEventsHandler {
    override fun onProposalReceived(proposal: WalletConnectEvents.SessionProposal, networksFormatted: String) {
        store.dispatchOnMain(
            GlobalAction.ShowDialog(
                WalletConnectDialog.SessionProposalDialog(
                    sessionProposal = proposal,
                    networks = networksFormatted,
                    onApprove = { store.dispatchOnMain(WalletConnectAction.ApproveProposal) },
                    onReject = { store.dispatchOnMain(WalletConnectAction.RejectProposal) },
                ),
            ),
        )
    }

    override fun onSessionEstablished() {
        store.dispatchOnMain(WalletConnectAction.SessionEstablished)
    }

    override fun onSessionRejected(error: WalletConnectError) {
        store.dispatchOnMain(WalletConnectAction.SessionRejected(error))
    }

    override fun onListOfSessionsUpdated(sessions: List<WcSessionForScreen>) {
        Timber.d("WC2: List of sessions updated. Sessions: $sessions")
        store.dispatchOnMain(WalletConnectAction.SessionListUpdated(sessions))
    }

    override fun onSessionRequest(request: WcPreparedRequest) {
        store.dispatchOnMain(WalletConnectAction.ShowSessionRequest(request))
    }

    override fun onUnsupportedRequest() {
        store.dispatchOnMain(WalletConnectAction.RejectUnsupportedRequest)
    }

    override fun onPairConnectError(error: Throwable) {
        store.dispatchOnMain(WalletConnectAction.PairConnectErrorAction(error))
    }
}