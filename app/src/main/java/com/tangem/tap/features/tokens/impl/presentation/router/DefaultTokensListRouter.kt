package com.tangem.tap.features.tokens.impl.presentation.router

import com.tangem.blockchain.common.Blockchain
import com.tangem.core.navigation.AppScreen
import com.tangem.core.navigation.NavigationAction
import com.tangem.tap.common.extensions.dispatchDialogShow
import com.tangem.tap.common.extensions.dispatchNotification
import com.tangem.tap.common.redux.AppDialog
import com.tangem.tap.features.customtoken.api.featuretoggles.CustomTokenFeatureToggles
import com.tangem.tap.features.tokens.legacy.redux.TokensAction
import com.tangem.tap.features.wallet.redux.models.WalletDialog
import com.tangem.tap.store
import com.tangem.wallet.R

/**
 * Default implementation of tokens list router
* [REDACTED_TODO_COMMENT]
 *
* [REDACTED_AUTHOR]
 */
internal class DefaultTokensListRouter(
    private val customTokenFeatureToggles: CustomTokenFeatureToggles,
) : TokensListRouter {

    override fun popBackStack() {
        store.dispatch(NavigationAction.PopBackTo())
    }

    override fun openAddCustomTokenScreen() {
        if (customTokenFeatureToggles.isRedesignedScreenEnabled) {
            store.dispatch(NavigationAction.NavigateTo(AppScreen.AddCustomToken))
        } else {
            store.dispatch(TokensAction.PrepareAndNavigateToAddCustomToken)
        }
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

    override fun openUnsupportedNetworkAlert(blockchain: Blockchain) {
        val alert = when (blockchain) {
            Blockchain.Solana -> AppDialog.SimpleOkDialogRes(
                headerId = R.string.common_warning,
                messageId = R.string.alert_manage_tokens_unsupported_message,
            )
            Blockchain.Chia, Blockchain.ChiaTestnet -> AppDialog.SimpleOkDialogRes(
                headerId = R.string.common_warning,
                messageId = R.string.alert_manage_tokens_unsupported_curve_message,
                args = listOf(blockchain.fullName),
            )
            else -> null // there's no alerts for other blockchains yer
        }
        if (alert != null) {
            store.dispatchDialogShow(alert)
        }
    }
}
