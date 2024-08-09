package com.tangem.tap.features.customtoken.impl.presentation.routers

import com.tangem.blockchain.common.Blockchain
import com.tangem.common.routing.AppRoute
import com.tangem.common.routing.AppRouter
import com.tangem.common.routing.utils.popTo
import com.tangem.tap.common.extensions.dispatchDialogShow
import com.tangem.tap.common.extensions.dispatchNavigationAction
import com.tangem.tap.common.redux.AppDialog
import com.tangem.tap.store
import com.tangem.wallet.R

/** Default implementation of custom token feature router */
internal class DefaultCustomTokenRouter : CustomTokenRouter {

    override fun popBackStack() {
        store.dispatchNavigationAction(AppRouter::pop)
    }

    override fun openWalletScreen() {
        store.dispatchNavigationAction { popTo<AppRoute.Wallet>() }
    }

    override fun openUnsupportedNetworkAlert(blockchain: Blockchain) {
        val alert = AppDialog.SimpleOkDialogRes(
            headerId = R.string.common_warning,
            messageId = R.string.alert_manage_tokens_unsupported_curve_message,
            args = listOf(blockchain.getNetworkName()),
        )
        store.dispatchDialogShow(alert)
    }

    override fun showGenericErrorAlertAndPopBack() {
        val alert = AppDialog.SimpleOkDialogRes(
            headerId = R.string.common_error,
            messageId = R.string.common_unknown_error,
            onOk = { store.dispatchNavigationAction(AppRouter::pop) },
        )
        store.dispatchDialogShow(alert)
    }
}
