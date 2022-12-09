package com.tangem.tap.features.details.ui.walletconnect.dialogs

import android.content.Context
import androidx.appcompat.app.AlertDialog
import com.tangem.blockchain.common.Blockchain
import com.tangem.tap.common.redux.global.GlobalAction
import com.tangem.tap.features.details.redux.walletconnect.WalletConnectAction
import com.tangem.tap.features.details.redux.walletconnect.WalletConnectSession
import com.tangem.tap.store
import com.tangem.wallet.R

class ApproveWcSessionDialog {
    companion object {
        fun create(session: WalletConnectSession, networks: List<Blockchain>, context: Context): AlertDialog {
            val sessionBlockchain = requireNotNull(session.wallet.blockchain) { "session network is null" }
            val message = context.getString(
                R.string.wallet_connect_request_session_start,
                session.peerMeta.name,
                sessionBlockchain.fullName,
                session.peerMeta.url,
            )
            return AlertDialog.Builder(context).apply {
                setTitle(context.getString(R.string.wallet_connect_title))
                setMessage(message)
                setPositiveButton(context.getText(R.string.common_start)) { _, _ ->
                    store.dispatch(GlobalAction.HideDialog)
                    store.dispatch(WalletConnectAction.ChooseNetwork(sessionBlockchain))
                }
                if (networks.size > 1) {
                    setNeutralButton(context.getText(R.string.wallet_connect_select_network)) { _, _ ->
                        store.dispatch(GlobalAction.HideDialog)
                        store.dispatch(WalletConnectAction.SelectNetwork(session = session, networks = networks))
                    }
                }
                setNegativeButton(context.getText(R.string.common_reject)) { _, _ ->
                    store.dispatch(GlobalAction.HideDialog)
                    store.dispatch(WalletConnectAction.FailureEstablishingSession(session.session))
                }
                setOnCancelListener {
                    store.dispatch(GlobalAction.HideDialog)
                    store.dispatch(WalletConnectAction.FailureEstablishingSession(session.session))
                }
            }.create()
        }
    }
}
