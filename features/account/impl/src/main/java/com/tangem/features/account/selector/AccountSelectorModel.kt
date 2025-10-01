package com.tangem.features.account.selector

import com.tangem.common.ui.account.AccountCryptoPortfolioItemStateConverter
import com.tangem.core.decompose.di.ModelScoped
import com.tangem.core.decompose.model.Model
import com.tangem.core.decompose.model.ParamsContainer
import com.tangem.core.ui.components.token.state.TokenItemState
import com.tangem.core.ui.extensions.stringReference
import com.tangem.domain.core.lce.Lce
import com.tangem.domain.core.utils.getOrElse
import com.tangem.domain.models.TotalFiatBalance
import com.tangem.domain.models.account.Account
import com.tangem.domain.models.wallet.isLocked
import com.tangem.domain.tokens.error.TokenListError
import com.tangem.features.account.AccountSelectorComponent
import com.tangem.features.account.AccountsBalanceFetcher
import com.tangem.features.account.selector.entity.AccountSelectorItemUM
import com.tangem.features.account.selector.entity.AccountSelectorUM
import com.tangem.utils.coroutines.CoroutineDispatcherProvider
import kotlinx.collections.immutable.persistentListOf
import kotlinx.collections.immutable.toImmutableList
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.launchIn
import kotlinx.coroutines.flow.map
import javax.inject.Inject

@ModelScoped
internal class AccountSelectorModel @Inject constructor(
    paramsContainer: ParamsContainer,
    override val dispatchers: CoroutineDispatcherProvider,
) : Model() {

    private val params = paramsContainer.require<AccountSelectorComponent.Params>()
    private val balanceFetcher get() = params.accountsBalanceFetcher
    private val selectorController get() = params.controller

    internal val state: StateFlow<AccountSelectorUM>
    field = MutableStateFlow<AccountSelectorUM>(emptyState())

    init {
        balanceFetcher.data
            .map { data ->
                AccountSelectorUM(
                    isSingleWallet = balanceFetcher.mode.value is AccountsBalanceFetcher.Mode.Wallet,
                    items = buildUiList(data).toImmutableList(),
                )
            }
            .launchIn(modelScope)
    }

    private fun buildUiList(data: AccountsBalanceFetcher.Data) = buildList {
        fun Account.accountItemState(balance: Lce<TokenListError, TotalFiatBalance>): TokenItemState {
            val totalFiatBalance = balance.getOrElse(
                ifError = { TotalFiatBalance.Failed },
                ifLoading = { TotalFiatBalance.Loading },
            )
            return when (this) {
                is Account.CryptoPortfolio -> AccountCryptoPortfolioItemStateConverter(
                    appCurrency = data.appCurrency,
                    account = this,
                    onItemClick = { selectorController.selectAccount(it) },
                ).convert(totalFiatBalance)
            }
        }

        data.balances.forEach { wallet, accounts ->
            if (wallet.isLocked) return@forEach
            AccountSelectorItemUM.Wallet(
                id = wallet.walletId.stringValue,
                name = stringReference(wallet.name),
            ).let(::add)

            accounts.forEach { account, balance ->
                AccountSelectorItemUM.Account(
                    account = account.accountItemState(balance.balance),
                    isBalanceHidden = data.isBalanceHidden,
                ).let(::add)
            }
        }
    }

    private fun emptyState() = AccountSelectorUM(
        items = persistentListOf(),
        balanceFetcher.mode.value is AccountsBalanceFetcher.Mode.Wallet,
    )
}