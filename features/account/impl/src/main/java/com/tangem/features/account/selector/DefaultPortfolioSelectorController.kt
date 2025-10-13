package com.tangem.features.account.selector

import com.tangem.domain.account.usecase.IsAccountsModeEnabledUseCase
import com.tangem.domain.models.account.AccountId
import com.tangem.domain.models.account.AccountStatus
import com.tangem.domain.models.wallet.UserWallet
import com.tangem.features.account.PortfolioSelectorController
import com.tangem.features.account.PortfolioFetcher
import kotlinx.coroutines.flow.*
import javax.inject.Inject

internal class DefaultPortfolioSelectorController @Inject constructor(
    private val isAccountsModeEnabledUseCase: IsAccountsModeEnabledUseCase,
) : PortfolioSelectorController {

    private val _selectedAccount: MutableStateFlow<AccountId?> = MutableStateFlow(null)

    override val isAccountMode: Flow<Boolean> by lazy { isAccountsModeEnabledUseCase() }

    override val selectedAccount: StateFlow<AccountId?> get() = _selectedAccount

    override fun selectAccount(accountId: AccountId?) {
        _selectedAccount.update { accountId }
    }

    override fun selectedAccountWithData(portfolioFetcher: PortfolioFetcher): Flow<Pair<UserWallet, AccountStatus>?> =
        combine(
            flow = _selectedAccount,
            flow2 = portfolioFetcher.data,
            transform = { accountId, data ->
                accountId ?: return@combine null
                var result: Pair<UserWallet, AccountStatus>? = null

                data.balances.forEach { wallet, balance ->
                    val accountStatuses = balance.accountsBalance.accountStatuses
                        .find { accountId == it.account.accountId }
                    if (accountStatuses != null) result = wallet to accountStatuses
                }

                return@combine result
            },
        )
}