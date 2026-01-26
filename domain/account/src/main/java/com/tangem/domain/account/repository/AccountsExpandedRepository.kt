package com.tangem.domain.account.repository

import com.tangem.domain.account.models.AccountExpandedState
import com.tangem.domain.models.account.AccountId
import com.tangem.domain.models.wallet.UserWalletId
import kotlinx.coroutines.flow.Flow

interface AccountsExpandedRepository {

    val expandedAccounts: Flow<Map<UserWalletId, Set<AccountExpandedState>>>

    suspend fun syncStore(walletId: UserWalletId, existAccounts: Set<AccountId>)
    suspend fun update(accountState: AccountExpandedState)
}