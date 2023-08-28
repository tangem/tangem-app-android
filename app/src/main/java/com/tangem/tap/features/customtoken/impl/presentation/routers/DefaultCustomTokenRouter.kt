package com.tangem.tap.features.customtoken.impl.presentation.routers

import com.tangem.blockchain.common.Blockchain
import com.tangem.core.navigation.AppScreen
import com.tangem.core.navigation.NavigationAction
import com.tangem.tap.common.extensions.dispatchDialogShow
import com.tangem.tap.common.redux.AppDialog
import com.tangem.tap.store
import com.tangem.wallet.R

/** Default implementation of custom token feature router */
internal class DefaultCustomTokenRouter : CustomTokenRouter {

    override fun popBackStack() {
        store.dispatch(NavigationAction.PopBackTo())
    }

    override fun openWalletScreen() {
        store.dispatch(NavigationAction.PopBackTo(screen = AppScreen.Wallet))
    }

    override fun openUnsupportedNetworkAlert(blockchain: Blockchain) {
        val alert = AppDialog.SimpleOkDialogRes(
            headerId = R.string.common_warning,
            messageId = R.string.alert_manage_tokens_unsupported_curve_message,
            args = listOf(blockchain.fullName),
        )
        store.dispatchDialogShow(alert)
    }
}