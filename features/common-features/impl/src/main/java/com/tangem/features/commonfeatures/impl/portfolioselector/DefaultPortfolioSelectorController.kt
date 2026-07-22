package com.tangem.features.commonfeatures.impl.portfolioselector

import com.tangem.domain.account.status.usecase.IsAccountsModeEnabledUseCase
import com.tangem.domain.models.account.AccountId
import com.tangem.domain.models.account.AccountStatus
import com.tangem.domain.models.account.filterCryptoPortfolio
import com.tangem.domain.models.wallet.UserWallet
import com.tangem.features.commonfeatures.api.portfolioselector.PortfolioFetcher
import com.tangem.features.commonfeatures.api.portfolioselector.PortfolioSelectorController
import kotlinx.coroutines.channels.BufferOverflow
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.combine
import javax.inject.Inject

internal class DefaultPortfolioSelectorController @Inject constructor(
    private val isAccountsModeEnabledUseCase: IsAccountsModeEnabledUseCase,
) : PortfolioSelectorController {

    private val _selectedAccount: MutableSharedFlow<Set<AccountId>> = MutableSharedFlow(
        replay = 1,
        onBufferOverflow = BufferOverflow.DROP_OLDEST,
    )

    override val isAccountMode: Flow<Boolean> by lazy { isAccountsModeEnabledUseCase() }
    // without StateFlow and distinctUntilChanged to allow reselect and correct navigation
    override val selectedAccounts: Flow<Set<AccountId>> get() = _selectedAccount
    override val selectedAccountsSync: Set<AccountId> get() = _selectedAccount.replayCache.firstOrNull().orEmpty()

    override val isEnabled: MutableStateFlow<(UserWallet, AccountStatus) -> Boolean> = MutableStateFlow { _, _ -> true }

    override suspend fun isAccountModeSync(): Boolean = isAccountsModeEnabledUseCase.invokeSync()

    override fun selectAccount(accountIds: Set<AccountId>) {
        _selectedAccount.tryEmit(accountIds)
    }

    override fun selectedAccountsWithData(
        portfolioFetcher: PortfolioFetcher,
    ): Flow<Set<Pair<UserWallet, AccountStatus.CryptoPortfolio>>> = combine(
        flow = _selectedAccount,
        flow2 = portfolioFetcher.data,
        transform = { accountIds, data ->
            val result = mutableSetOf<Pair<UserWallet, AccountStatus.CryptoPortfolio>>()

            data.balances.forEach { (_, balance) ->
                val accounts = balance.accountsBalance.accountStatuses
                    .filterCryptoPortfolio()
                    .filterTo(mutableSetOf()) { it.account.accountId in accountIds }
                    .map { balance.userWallet to it }
                result.addAll(accounts)
            }

            return@combine result
        },
    )
}