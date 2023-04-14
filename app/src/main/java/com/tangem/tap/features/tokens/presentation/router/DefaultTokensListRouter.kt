package com.tangem.tap.features.tokens.presentation.router

import com.tangem.tap.common.extensions.dispatchDialogShow
import com.tangem.tap.common.extensions.dispatchNotification
import com.tangem.tap.common.redux.AppDialog
import com.tangem.tap.common.redux.navigation.NavigationAction
import com.tangem.tap.features.tokens.redux.TokensAction
import com.tangem.tap.features.wallet.redux.models.WalletDialog
import com.tangem.tap.store
import com.tangem.wallet.R

/**
 * Default implementation of tokens list router
 * FIXME("Necessary to avoid using redux actions")
 *
[REDACTED_AUTHOR]
 */
internal class DefaultTokensListRouter : TokensListRouter {

    override fun popBackStack() {
        store.dispatch(NavigationAction.PopBackTo())
        store.dispatch(TokensAction.ResetState)
    }

    override fun openAddCustomTokenScreen() {
        store.dispatch(TokensAction.PrepareAndNavigateToAddCustomToken)
    }

    override fun showAddressCopiedNotification() {
        store.dispatchNotification(R.string.contract_address_copied_message)
    }

    override fun openUnableHideMainTokenAlert(tokenName: String, tokenSymbol: String) {
        store.dispatchDialogShow(
            dialog = WalletDialog.TokensAreLinkedDialog(currencyTitle = tokenName, currencySymbol = tokenSymbol),
        )
    }

    override fun openRemoveWalletAlert(tokenName: String, onOkClick: () -> Unit) {
        store.dispatchDialogShow(
            dialog = WalletDialog.RemoveWalletDialog(currencyTitle = tokenName, onOk = onOkClick),
        )
    }

    override fun openUnsupportedSoltanaNetworkAlert() {
        store.dispatchDialogShow(
            AppDialog.SimpleOkDialogRes(
                headerId = R.string.common_warning,
                messageId = R.string.alert_manage_tokens_unsupported_message,
            ),
        )
    }
}