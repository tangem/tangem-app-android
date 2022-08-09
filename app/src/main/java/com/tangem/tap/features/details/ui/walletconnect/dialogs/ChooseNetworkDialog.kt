package com.tangem.tap.features.details.ui.walletconnect.dialogs

import android.content.Context
import androidx.appcompat.app.AlertDialog
import com.tangem.blockchain.common.Blockchain
import com.tangem.tap.common.redux.global.GlobalAction
import com.tangem.tap.features.details.redux.walletconnect.WalletConnectAction
import com.tangem.tap.store
import com.tangem.wallet.R

object ChooseNetworkDialog {
    fun create(
        blockchains: List<Blockchain>,
        context: Context,
    ): AlertDialog {
        return AlertDialog.Builder(context)
            .setTitle(context.getString(R.string.wallet_connect_select_network))
            .setNegativeButton(context.getString(R.string.common_cancel)) { _, _ -> /* no-op */ }
            .setOnDismissListener {
                store.dispatch(GlobalAction.HideDialog())
            }
            .setSingleChoiceItems(blockchains.map { it.fullName }.toTypedArray(), 0) { _, which ->
                blockchains.getOrNull(which)?.let { selectedBlockchain ->
                    store.dispatch(
                        WalletConnectAction.ChooseNetwork(
                            blockchain = selectedBlockchain,
                        ),
                    )
                    store.dispatch(GlobalAction.HideDialog())
                }
            }
            .create()
    }
}