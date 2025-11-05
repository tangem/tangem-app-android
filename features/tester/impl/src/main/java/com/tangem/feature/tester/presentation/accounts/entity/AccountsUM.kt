package com.tangem.feature.tester.presentation.accounts.entity

import com.tangem.core.ui.components.bottomsheets.TangemBottomSheetConfigContent
import com.tangem.domain.models.account.Account
import com.tangem.domain.models.wallet.UserWallet
import kotlinx.collections.immutable.ImmutableList

internal data class AccountsUM(
    val onBackClick: () -> Unit,
    val walletSelector: WalletSelector,
    val accountListBottomSheetConfig: AccountListBottomSheetConfig,
    val buttons: ImmutableList<Button>,
) {

    data class Button(
        val title: String,
        val isInProgress: Boolean = false,
        val isEnabled: Boolean = true,
        val onClick: (title: String) -> Unit,
    )

    data class WalletSelector(
        val selected: UserWallet?,
        val wallets: ImmutableList<UserWallet>,
        val onWalletSelect: (UserWallet) -> Unit,
    ) : TangemBottomSheetConfigContent

    data class AccountListBottomSheetConfig(
        val isAccountsShown: Boolean,
        val accounts: ImmutableList<Account.CryptoPortfolio>,
        val onDismiss: () -> Unit,
    ) : TangemBottomSheetConfigContent
}