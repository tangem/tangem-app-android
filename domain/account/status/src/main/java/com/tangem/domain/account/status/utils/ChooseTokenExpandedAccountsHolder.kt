package com.tangem.domain.account.status.utils

import com.tangem.domain.account.repository.AccountsExpandedRepository
import com.tangem.domain.models.account.AccountId
import com.tangem.domain.models.wallet.UserWalletId
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.emitAll
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.flow
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class ChooseTokenExpandedAccountsHolder @Inject constructor(
    private val mainHolder: MainExpandedAccountsHolder,
    holderFactory: DefaultExpandedAccountsHolder.Factory,
    repositoryFactory: AccountsExpandedRepository.Factory,
) : ExpandedAccountsHolder {

    private val repository: AccountsExpandedRepository =
        repositoryFactory.create(AccountsExpandedRepository.CHOOSE_TOKEN_FILE_NAME)
    private val defaultHolder: DefaultExpandedAccountsHolder = holderFactory.create(repository)

    override fun expandedAccounts(walletId: UserWalletId): Flow<Set<AccountId>> = flow {
        val isStored = repository.expandedAccounts.first()[walletId] != null

        if (isStored) {
            emitAll(defaultHolder.expandedAccounts(walletId))
        } else {
            val initExpanded = mainHolder.expandedAccounts(walletId).first()
            emitAll(defaultHolder.expandedAccounts(walletId, initExpanded))
        }
    }

    override fun expandAccount(accountId: AccountId) {
        defaultHolder.expandAccount(accountId)
    }

    override fun collapseAccount(accountId: AccountId) {
        defaultHolder.collapseAccount(accountId)
    }
}