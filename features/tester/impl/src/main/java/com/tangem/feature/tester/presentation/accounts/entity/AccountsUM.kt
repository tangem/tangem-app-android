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
        val id: ID,
        val title: String = createTitle(id),
        val isInProgress: Boolean = false,
        val isEnabled: Boolean = true,
        val onClick: () -> Unit,
    ) {

        enum class ID {
            ShowAccountList,
            FetchAccounts,
            FillOutList,
            FillOutArchivedList,
            ArchiveAll,
            SortByDerivationIndex,
            ClearETag,
        }

        fun reset(): Button {
            return copy(title = createTitle(id), isInProgress = false, isEnabled = true)
        }

        companion object {

            private fun createTitle(id: ID): String = when (id) {
                ID.ShowAccountList -> "Show the account list"
                ID.FetchAccounts -> "Fetch accounts"
                ID.FillOutList -> "Fill out the list (up to 20)"
                ID.FillOutArchivedList -> "Fill out the archived list"
                ID.ArchiveAll -> "Archive all"
                ID.SortByDerivationIndex -> "Sort by derivation index"
                ID.ClearETag -> "Clear ETag"
            }
        }
    }

    data class WalletSelector(
        val selected: UserWallet?,
        val wallets: ImmutableList<UserWallet>,
        val onWalletSelect: (UserWallet) -> Unit,
    ) : TangemBottomSheetConfigContent

    data class AccountListBottomSheetConfig(
        val isAccountsShown: Boolean,
        val accounts: ImmutableList<Account.Crypto>,
        val onDismiss: () -> Unit,
    ) : TangemBottomSheetConfigContent
}