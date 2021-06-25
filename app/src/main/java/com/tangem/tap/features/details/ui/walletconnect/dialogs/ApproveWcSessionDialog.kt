package com.tangem.tap.features.details.ui.walletconnect.dialogs

import android.content.Context
import androidx.appcompat.app.AlertDialog
import com.tangem.tap.common.redux.global.GlobalAction
import com.tangem.tap.features.details.redux.walletconnect.WalletConnectAction
import com.tangem.tap.features.details.redux.walletconnect.WalletConnectSession
import com.tangem.tap.store
import com.tangem.wallet.R

class ApproveWcSessionDialog {
    companion object {
        fun create(session: WalletConnectSession, context: Context): AlertDialog {
            val message = context.getString(
                R.string.wallet_connect_request_session_start,
                session.wallet.cardId,
                session.peerMeta.name,
                session.peerMeta.url
            )
            return AlertDialog.Builder(context).apply {
                setTitle(context.getString(R.string.wallet_connect))
                setMessage(message)
                setPositiveButton(context.getText(R.string.common_start)) { _, _ ->
                    store.dispatch(WalletConnectAction.ApproveSession(
                        session.session
                    ))
                }
                setNegativeButton(context.getText(R.string.common_reject)) { _, _ ->
                    store.dispatch(WalletConnectAction.FailureEstablishingSession(session.session))
                }
                setOnDismissListener {
                    store.dispatch(GlobalAction.HideDialog)
                }
            }.create()
        }
    }
}