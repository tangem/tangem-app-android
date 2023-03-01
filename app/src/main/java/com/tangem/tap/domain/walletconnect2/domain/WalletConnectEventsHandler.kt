package com.tangem.tap.domain.walletconnect2.domain

import com.tangem.tap.domain.walletconnect2.domain.models.WalletConnectError
import com.tangem.tap.domain.walletconnect2.domain.models.WalletConnectEvents
import com.tangem.tap.features.details.ui.walletconnect.WcSessionForScreen

interface WalletConnectEventsHandler {
    fun onProposalReceived(proposal: WalletConnectEvents.SessionProposal, networksFormatted: String)
    // val onProposalApproved: () -> Unit,
    // val onProposalDeclined: () -> Unit,

    fun onSessionEstablished()

    fun onSessionRejected(error: WalletConnectError)

    fun onListOfSessionsUpdated(sessions: List<WcSessionForScreen>)

    fun onSessionRequest(request: WcPreparedRequest)

    fun onSessionRequestForWrongUserWallet()

    fun onUnsupportedRequest()
}
