package com.tangem.data.walletconnect.utils

import com.reown.walletkit.client.Wallet
import com.reown.walletkit.client.WalletKit

internal interface WcSdkObserver : WalletKit.WalletDelegate {

    override val onSessionAuthenticate: ((Wallet.Model.SessionAuthenticate, Wallet.Model.VerifyContext) -> Unit)?
        get() = super.onSessionAuthenticate

    override fun onConnectionStateChange(state: Wallet.Model.ConnectionState) {}

    override fun onError(error: Wallet.Model.Error) {}

    override fun onProposalExpired(proposal: Wallet.Model.ExpiredProposal) {}

    override fun onRequestExpired(request: Wallet.Model.ExpiredRequest) {}

    override fun onSessionDelete(sessionDelete: Wallet.Model.SessionDelete) {}

    override fun onSessionExtend(session: Wallet.Model.Session) {}

    override fun onSessionProposal(
        sessionProposal: Wallet.Model.SessionProposal,
        verifyContext: Wallet.Model.VerifyContext,
    ) {
    }

    override fun onSessionRequest(
        sessionRequest: Wallet.Model.SessionRequest,
        verifyContext: Wallet.Model.VerifyContext,
    ) {
    }

    override fun onSessionSettleResponse(settleSessionResponse: Wallet.Model.SettledSessionResponse) {}

    override fun onSessionUpdateResponse(sessionUpdateResponse: Wallet.Model.SessionUpdateResponse) {}
}