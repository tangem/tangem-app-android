package com.tangem.feature.walletsettings.utils

import com.tangem.common.routing.AppRoute
import com.tangem.common.ui.account.AccountPortfolioItemUMConverter
import com.tangem.core.analytics.api.AnalyticsEventHandler
import com.tangem.core.decompose.di.ModelScoped
import com.tangem.core.decompose.model.ParamsContainer
import com.tangem.core.decompose.navigation.Router
import com.tangem.core.decompose.ui.UiMessageSender
import com.tangem.core.ui.components.block.model.BlockUM
import com.tangem.core.ui.extensions.resourceReference
import com.tangem.core.ui.extensions.wrappedList
import com.tangem.core.ui.message.DialogMessage
import com.tangem.core.ui.message.EventMessageAction
import com.tangem.domain.account.featuretoggle.AccountsFeatureToggles
import com.tangem.domain.account.models.AccountList
import com.tangem.domain.account.models.AccountStatusList
import com.tangem.domain.account.status.supplier.SingleAccountStatusListSupplier
import com.tangem.domain.appcurrency.GetSelectedAppCurrencyUseCase
import com.tangem.domain.appcurrency.model.AppCurrency
import com.tangem.domain.balancehiding.GetBalanceHidingSettingsUseCase
import com.tangem.domain.models.account.Account
import com.tangem.domain.models.account.AccountId
import com.tangem.domain.models.account.AccountStatus
import com.tangem.domain.models.wallet.UserWallet
import com.tangem.domain.models.wallet.UserWalletId
import com.tangem.domain.wallets.analytics.WalletSettingsAnalyticEvents
import com.tangem.domain.wallets.extension.isAccountsSupported
import com.tangem.feature.walletsettings.component.WalletSettingsComponent
import com.tangem.feature.walletsettings.entity.WalletSettingsAccountsUM
import com.tangem.feature.walletsettings.entity.WalletSettingsAccountsUM.Footer.AddAccountUM
import com.tangem.feature.walletsettings.impl.R
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.flowOf
import javax.inject.Inject

@Suppress("LongParameterList")
@ModelScoped
internal class AccountItemsDelegate @Inject constructor(
    paramsContainer: ParamsContainer,
    private val router: Router,
    private val messageSender: UiMessageSender,
    private val singleAccountStatusListSupplier: SingleAccountStatusListSupplier,
    private val getBalanceHidingSettingsUseCase: GetBalanceHidingSettingsUseCase,
    private val getSelectedAppCurrencyUseCase: GetSelectedAppCurrencyUseCase,
    private val accountListSortingSaver: AccountListSortingSaver,
    private val accountsFeatureToggles: AccountsFeatureToggles,
    private val analyticsEventHandler: AnalyticsEventHandler,
) {

    private val userWalletId = paramsContainer.require<WalletSettingsComponent.Params>().userWalletId

    fun isAccountsSupported(wallet: UserWallet) = accountsFeatureToggles.isFeatureEnabled && wallet.isAccountsSupported

    fun loadAccount(wallet: UserWallet): Flow<List<WalletSettingsAccountsUM>> {
        if (!isAccountsSupported(wallet)) return flowOf(emptyList())

        return combine(
            flow = singleAccountStatusListSupplier(userWalletId),
            flow2 = getSelectedAppCurrencyUseCase.invokeOrDefault(),
            flow3 = getBalanceHidingSettingsUseCase.isBalanceHidden(),
            flow4 = accountListSortingSaver.accountsOrderFlow,
            transform = ::buildUiList,
        )
    }

    private fun buildUiList(
        accountStatusList: AccountStatusList,
        appCurrency: AppCurrency,
        isBalanceHidden: Boolean,
        accountsOrder: List<AccountId>?,
    ): List<WalletSettingsAccountsUM> = buildList {
        fun AccountStatus.CryptoPortfolio.mapCryptoPortfolio(): WalletSettingsAccountsUM {
            val accountItemUM = AccountPortfolioItemUMConverter(
                onClick = {
                    analyticsEventHandler.send(WalletSettingsAnalyticEvents.ButtonOpenExistingAccount())
                    openAccountDetails(this.account)
                },
                appCurrency = appCurrency,
                accountBalance = this.tokenList.totalFiatBalance,
                isBalanceHidden = isBalanceHidden,
            ).convert(account)
            return WalletSettingsAccountsUM.Account(state = accountItemUM)
        }

        fun mapAccount(account: AccountStatus): WalletSettingsAccountsUM = when (account) {
            is AccountStatus.CryptoPortfolio -> account.mapCryptoPortfolio()
        }

        val accounts = accountStatusList.accountStatuses

        val header = WalletSettingsAccountsUM.Header(
            id = "accounts_header",
            text = resourceReference(R.string.common_accounts),
        )

        add(header)
        addAll(accounts.map(::mapAccount).applySortingOrder(order = accountsOrder))

        val isAddAccountEnabled = accounts.size < AccountList.MAX_ACCOUNTS_COUNT
        val shouldShowDescription = accounts.size > 1
        val isArchivedAccountsEnabled = accountStatusList.accountStatuses.size != accountStatusList.totalAccounts

        val footer = WalletSettingsAccountsUM.Footer(
            id = "accounts_footer",
            addAccount = AddAccountUM(
                title = resourceReference(R.string.account_form_title_create),
                isAddAccountEnabled = isAddAccountEnabled,
                onAddAccountClick = {
                    if (isAddAccountEnabled) {
                        analyticsEventHandler.send(WalletSettingsAnalyticEvents.ButtonAddAccount())
                        openAddAccount(userWalletId)
                    } else {
                        canNotAddAccountDialog()
                    }
                },
            ),
            archivedAccounts = if (isArchivedAccountsEnabled) {
                BlockUM(
                    text = resourceReference(R.string.account_archived_accounts),
                    iconRes = R.drawable.ic_archive_24,
                    onClick = {
                        analyticsEventHandler.send(WalletSettingsAnalyticEvents.ButtonArchivedAccounts())
                        openArchivedAccounts(userWalletId)
                    },
                )
            } else {
                null
            },
            shouldShowDescription = shouldShowDescription,
            description = resourceReference(R.string.account_reorder_description),
        )

        add(footer)
    }

    private fun List<WalletSettingsAccountsUM>.applySortingOrder(
        order: List<AccountId>?,
    ): List<WalletSettingsAccountsUM> {
        if (order == null) return this

        val positionByAccountId = order.withIndex().associate { it.value.value to it.index }
        return this.sortedBy { positionByAccountId[it.id] ?: Int.MAX_VALUE }
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
                    formatArgs = wrappedList(AccountList.MAX_ACCOUNTS_COUNT.toString()),
                ),
                firstActionBuilder = { firstAction },
            ),
        )
    }
}