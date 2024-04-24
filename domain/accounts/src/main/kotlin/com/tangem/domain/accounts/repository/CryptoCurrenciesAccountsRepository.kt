package com.tangem.domain.accounts.repository

import com.tangem.domain.accounts.model.CryptoCurrenciesAccount
import com.tangem.domain.core.lce.LceFlow
import com.tangem.domain.wallets.models.UserWalletId
import kotlinx.coroutines.flow.Flow

interface CryptoCurrenciesAccountsRepository {

    fun getAccountsUpdates(userWalletId: UserWalletId): LceFlow<Throwable, List<CryptoCurrenciesAccount>>

    fun getSelectedAccountIdUpdates(userWalletId: UserWalletId): Flow<CryptoCurrenciesAccount.ID>

    suspend fun getAccounts(userWalletId: UserWalletId, refresh: Boolean = false): List<CryptoCurrenciesAccount>

    suspend fun selectAccountIfPresent(userWalletId: UserWalletId, accountId: CryptoCurrenciesAccount.ID): Boolean

    suspend fun createAccountIfNot(userWalletId: UserWalletId, account: CryptoCurrenciesAccount): Boolean

    suspend fun archiveAccount(userWalletId: UserWalletId, accountId: CryptoCurrenciesAccount.ID): Boolean

    suspend fun restoreAccount(userWalletId: UserWalletId, accountId: CryptoCurrenciesAccount.ID): Boolean

    suspend fun changeAccountTitle(
        userWalletId: UserWalletId,
        accountId: CryptoCurrenciesAccount.ID,
        newTitle: CryptoCurrenciesAccount.Title.Default,
    ): Boolean
}