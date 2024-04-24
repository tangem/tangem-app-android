package com.tangem.data.accounts

import com.tangem.domain.accounts.model.CryptoCurrenciesAccount
import com.tangem.domain.accounts.repository.CryptoCurrenciesAccountsRepository
import com.tangem.domain.core.lce.LceFlow
import com.tangem.domain.wallets.models.UserWalletId
import kotlinx.coroutines.flow.Flow

internal class NotImplementedAccountsRepository : CryptoCurrenciesAccountsRepository {

    override fun getAccountsUpdates(userWalletId: UserWalletId): LceFlow<Throwable, List<CryptoCurrenciesAccount>> {
        TODO("Not yet implemented")
    }

    override fun getSelectedAccountIdUpdates(userWalletId: UserWalletId): Flow<CryptoCurrenciesAccount.ID> {
        TODO("Not yet implemented")
    }

    override suspend fun getAccounts(userWalletId: UserWalletId, refresh: Boolean): List<CryptoCurrenciesAccount> {
        TODO("Not yet implemented")
    }

    override suspend fun selectAccountIfPresent(
        userWalletId: UserWalletId,
        accountId: CryptoCurrenciesAccount.ID,
    ): Boolean {
        TODO("Not yet implemented")
    }

    override suspend fun createAccountIfNot(userWalletId: UserWalletId, account: CryptoCurrenciesAccount): Boolean {
        TODO("Not yet implemented")
    }

    override suspend fun archiveAccount(userWalletId: UserWalletId, accountId: CryptoCurrenciesAccount.ID): Boolean {
        TODO("Not yet implemented")
    }

    override suspend fun restoreAccount(userWalletId: UserWalletId, accountId: CryptoCurrenciesAccount.ID): Boolean {
        TODO("Not yet implemented")
    }

    override suspend fun changeAccountTitle(
        userWalletId: UserWalletId,
        accountId: CryptoCurrenciesAccount.ID,
        newTitle: CryptoCurrenciesAccount.Title.Default,
    ): Boolean {
        TODO("Not yet implemented")
    }
}