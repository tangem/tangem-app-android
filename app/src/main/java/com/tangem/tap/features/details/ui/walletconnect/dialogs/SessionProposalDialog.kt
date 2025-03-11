package com.tangem.tap.features.details.ui.walletconnect.dialogs

import android.content.Context
import androidx.appcompat.app.AlertDialog
import com.google.android.material.dialog.MaterialAlertDialogBuilder
import com.tangem.tap.common.redux.global.GlobalAction
import com.tangem.tap.domain.walletconnect2.domain.models.WalletConnectEvents
import com.tangem.tap.store
import com.tangem.wallet.R

object SessionProposalDialog {
    fun create(
        sessionProposal: WalletConnectEvents.SessionProposal,
        networks: String,
        context: Context,
        onApprove: () -> Unit,
        onReject: () -> Unit,
    ): AlertDialog {
        val message = context.getString(
            R.string.wallet_connect_request_session_start,
            sessionProposal.name,
            networks,
            sessionProposal.url,
        )
        return MaterialAlertDialogBuilder(context, R.style.CustomMaterialDialog).apply {
            setTitle(context.getString(R.string.wallet_connect_title))
            setMessage(message)
            setPositiveButton(context.getText(R.string.common_start)) { _, _ ->
                store.dispatch(GlobalAction.HideDialog)
                onApprove()
            }
            setNegativeButton(context.getText(R.string.common_reject)) { _, _ ->
                store.dispatch(GlobalAction.HideDialog)
                onReject()
            }
            setOnCancelListener {
                store.dispatch(GlobalAction.HideDialog)
                onReject()
            }
        }.create()
    }
}