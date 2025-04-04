package com.tangem.tap.features.details.redux.walletconnect

import com.tangem.tap.domain.walletconnect2.domain.WcPreparedRequest
import com.tangem.tap.domain.walletconnect2.domain.models.WalletConnectError
import com.tangem.tap.domain.walletconnect2.domain.models.WalletConnectEvents
import com.tangem.tap.features.details.ui.walletconnect.WcSessionForScreen
import org.rekotlin.Action

sealed class WalletConnectAction : Action {
    data class HandleDeepLink(val wcUri: String?) : WalletConnectAction()

    data object StartWalletConnect : WalletConnectAction()

    data class OpenSession(
        val wcUri: String,
        val source: SourceType,
    ) : WalletConnectAction() {
        enum class SourceType { QR, DEEPLINK, CLIPBOARD, ETC }
    }

    data class DisconnectSession(val topic: String) : WalletConnectAction()

    data class RejectRequest(val topic: String, val id: Long) : WalletConnectAction()

    //region WalletConnect 2.0
    data class ApproveProposal(val proposal: WalletConnectEvents.SessionProposal) : WalletConnectAction()
    data object RejectProposal : WalletConnectAction()

    data object SessionEstablished : WalletConnectAction()
    data class SessionRejected(val error: WalletConnectError) : WalletConnectAction()
    data class SessionListUpdated(val sessions: List<WcSessionForScreen>) : WalletConnectAction()

    data class ShowSessionRequest(val sessionRequest: WcPreparedRequest) : WalletConnectAction()

    data object RejectUnsupportedRequest : WalletConnectAction()

    data class PerformRequestedAction(val sessionRequest: WcPreparedRequest) : WalletConnectAction()

    data class PairConnectErrorAction(val throwable: Throwable) : WalletConnectAction()

    data object UnsupportedDappRequest : WalletConnectAction()
    //endregion WalletConnect 2.0
}