package com.tangem.features.account.selector

import com.tangem.domain.account.usecase.IsAccountsModeEnabledUseCase
import com.tangem.domain.models.account.AccountId
import com.tangem.domain.models.account.AccountStatus
import com.tangem.domain.models.wallet.UserWallet
import com.tangem.features.account.PortfolioFetcher
import com.tangem.features.account.PortfolioSelectorController
import kotlinx.coroutines.channels.BufferOverflow
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.combine
import javax.inject.Inject

internal class DefaultPortfolioSelectorController @Inject constructor(
    private val isAccountsModeEnabledUseCase: IsAccountsModeEnabledUseCase,
) : PortfolioSelectorController {

    private val _selectedAccount: MutableSharedFlow<AccountId?> = MutableSharedFlow(
        replay = 1,
        onBufferOverflow = BufferOverflow.DROP_OLDEST,
    )

    override val isAccountMode: Flow<Boolean> by lazy { isAccountsModeEnabledUseCase() }
    // without StateFlow and distinctUntilChanged to allow reselect and correct navigation
    override val selectedAccount: Flow<AccountId?> get() = _selectedAccount
    override val selectedAccountSync: AccountId? get() = _selectedAccount.replayCache.firstOrNull()

    override val isEnabled: MutableStateFlow<(UserWallet, AccountStatus) -> Boolean> = MutableStateFlow { _, _ -> true }

    override fun selectAccount(accountId: AccountId?) {
        _selectedAccount.tryEmit(accountId)
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