package com.tangem.feature.wallet.presentation.account

import com.tangem.core.decompose.di.ModelScoped
import com.tangem.domain.account.models.AccountList
import com.tangem.domain.models.account.AccountId
import com.tangem.domain.models.wallet.UserWallet
import com.tangem.domain.models.wallet.UserWalletId
import kotlinx.coroutines.flow.*
import javax.inject.Inject

@ModelScoped
internal class ExpandedAccountsHolder @Inject constructor() {

    private val expandedAccounts = MutableStateFlow<Map<UserWalletId, Set<AccountId>>>(mapOf())

    fun expandedAccounts(userWallet: UserWallet): Flow<Set<AccountId>> = channelFlow {
        walletAccounts(userWallet)
            .onEach { accountList ->
                val isSingleAccount = accountList.totalAccounts == 1
                val defaultExpanded = when {
                    isSingleAccount -> setOf(accountList.mainAccount.accountId)
                    else -> setOf()
                }
                expandedAccounts.update { map ->
                    var expandedSet = map[userWallet.walletId] ?: defaultExpanded
                    // force expand for single account
                    if (isSingleAccount) expandedSet = defaultExpanded
                    map.plus(userWallet.walletId to expandedSet)
                }
            }.launchIn(this)

        expandedAccounts
            .mapNotNull { map -> map[userWallet.walletId] }
            .onEach { expanded -> channel.send(expanded) }
            .collect()
    }

    fun expandAccount(userWalletId: UserWalletId, accountId: AccountId) = expandedAccounts.update { map ->
        val expandedSet = map[userWalletId]?.plus(accountId) ?: return@update map
        map.plus(userWalletId to expandedSet)
    }

    fun collapseAccount(userWalletId: UserWalletId, accountId: AccountId) = expandedAccounts.update { map ->
        val expandedSet = map[userWalletId]?.minus(accountId) ?: return@update map
        map.plus(userWalletId to expandedSet)
    }

    private fun walletAccounts(userWallet: UserWallet): Flow<AccountList> {
        // todo accounts
        return flowOf(AccountList.empty(userWallet))
    }
}