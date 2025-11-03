package com.tangem.feature.tester.presentation.accounts.entity

import com.tangem.core.ui.components.bottomsheets.TangemBottomSheetConfigContent
import com.tangem.domain.models.account.Account
import com.tangem.domain.models.wallet.UserWallet
import kotlinx.collections.immutable.ImmutableList

internal data class AccountsUM(
    val onBackClick: () -> Unit,
    val walletSelector: WalletSelector,
    val accountListBottomSheetConfig: AccountListBottomSheetConfig,
    val onAccountsClick: () -> Boolean,
    val onFetchAccountsClick: () -> Unit,
    val onClearETagClick: () -> Unit,
) {

    data class WalletSelector(
        val selected: UserWallet?,
        val wallets: ImmutableList<UserWallet>,
        val onWalletSelect: (UserWallet) -> Unit,
    ) : TangemBottomSheetConfigContent

    data class AccountListBottomSheetConfig(
        val accounts: ImmutableList<Account.CryptoPortfolio>,
    ) : TangemBottomSheetConfigContent
}