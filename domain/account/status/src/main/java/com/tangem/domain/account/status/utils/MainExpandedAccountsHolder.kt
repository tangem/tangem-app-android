package com.tangem.domain.account.status.utils

import com.tangem.domain.account.repository.AccountsExpandedRepository
import com.tangem.domain.models.account.AccountId
import com.tangem.domain.models.wallet.UserWalletId
import kotlinx.coroutines.flow.Flow
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class MainExpandedAccountsHolder @Inject constructor(
    holderFactory: DefaultExpandedAccountsHolder.Factory,
    repositoryFactory: AccountsExpandedRepository.Factory,
) : ExpandedAccountsHolder {

    private val repository: AccountsExpandedRepository = repositoryFactory
        .create(AccountsExpandedRepository.MAIN_STORE_FILE_NAME)
    private val default: DefaultExpandedAccountsHolder = holderFactory.create(repository)

    override fun expandedAccounts(walletId: UserWalletId): Flow<Set<AccountId>> {
        return default.expandedAccounts(walletId)
    }

    override fun expandAccount(accountId: AccountId) {
        default.expandAccount(accountId)
    }

    override fun collapseAccount(accountId: AccountId) {
        default.collapseAccount(accountId)
    }
}