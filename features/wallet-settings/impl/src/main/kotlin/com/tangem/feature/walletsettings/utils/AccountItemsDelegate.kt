package com.tangem.feature.walletsettings.utils

import com.tangem.common.routing.AppRoute
import com.tangem.common.ui.account.toUM
import com.tangem.core.decompose.di.ModelScoped
import com.tangem.core.decompose.navigation.Router
import com.tangem.core.decompose.ui.UiMessageSender
import com.tangem.core.ui.components.block.model.BlockUM
import com.tangem.core.ui.extensions.pluralReference
import com.tangem.core.ui.extensions.resourceReference
import com.tangem.core.ui.extensions.wrappedList
import com.tangem.core.ui.message.DialogMessage
import com.tangem.core.ui.message.EventMessageAction
import com.tangem.domain.models.account.Account
import com.tangem.domain.models.wallet.UserWalletId
import com.tangem.feature.walletsettings.entity.WalletSettingsAccountsUM
import com.tangem.feature.walletsettings.entity.WalletSettingsAccountsUM.Footer.AddAccountUM
import com.tangem.feature.walletsettings.impl.R
import javax.inject.Inject

@ModelScoped
internal class AccountItemsDelegate @Inject constructor(
    private val router: Router,
    private val messageSender: UiMessageSender,
) {

    fun buildUiList(userWalletId: UserWalletId, accounts: List<Account>): List<WalletSettingsAccountsUM> = buildList {
        WalletSettingsAccountsUM.Header(
            id = "accounts_header",
            text = resourceReference(R.string.common_accounts),
        ).let(::add)

        addAll(accounts.map(::mapAccount))

        val addAccountEnabled = true // todo account
        WalletSettingsAccountsUM.Footer(
            id = "accounts_footer",
            addAccount = AddAccountUM(
                title = resourceReference(R.string.account_form_title_create),
                addAccountEnabled = addAccountEnabled,
                onAddAccountClick = {
                    if (addAccountEnabled) openAddAccount(userWalletId) else canNotAddAccountDialog()
                },
            ),
            archivedAccounts = BlockUM(
                text = resourceReference(R.string.account_archived_accounts),
                iconRes = R.drawable.ic_archive_24,
                onClick = { openArchivedAccounts(userWalletId) },
            ),
            description = resourceReference(R.string.account_reorder_description),
        ).let(::add)
    }

    private fun mapAccount(account: Account): WalletSettingsAccountsUM = when (account) {
        is Account.CryptoPortfolio -> account.mapCryptoPortfolio()
    }

    private fun Account.CryptoPortfolio.mapCryptoPortfolio(): WalletSettingsAccountsUM {
        return WalletSettingsAccountsUM.Account(
            id = accountId.value,
            accountName = accountName.toUM().value,
            accountIconUM = icon.toUM(),
            tokensInfo = pluralReference(
                R.plurals.common_tokens_count,
                count = tokensCount,
                formatArgs = wrappedList(tokensCount),
            ),
            networksInfo = pluralReference(
                R.plurals.common_networks_count,
                count = networksCount,
                formatArgs = wrappedList(networksCount),
            ),
            onClick = { openAccountDetails(this) },
        )
    }

    private fun openAccountDetails(account: Account) {
        router.push(AppRoute.AccountDetails(account))
    }

    private fun openArchivedAccounts(userWalletId: UserWalletId) {
        router.push(AppRoute.ArchivedAccountList(userWalletId))
    }

    private fun openAddAccount(userWalletId: UserWalletId) {
        router.push(AppRoute.CreateAccount(userWalletId))
    }

    private fun canNotAddAccountDialog() {
        val firstAction = EventMessageAction(
            title = resourceReference(R.string.common_got_it),
            onClick = { },
        )
        messageSender.send(
            DialogMessage(
                title = resourceReference(R.string.account_add_limit_dialog_title),
                message = resourceReference(
                    id = R.string.account_add_limit_dialog_description,
                    formatArgs = wrappedList(MAX_ACCOUNT_COUNT.toString()),
                ),
                firstActionBuilder = { firstAction },
            ),
        )
    }

    companion object {
        // todo account use domain const?
        private const val MAX_ACCOUNT_COUNT = 20
    }
}