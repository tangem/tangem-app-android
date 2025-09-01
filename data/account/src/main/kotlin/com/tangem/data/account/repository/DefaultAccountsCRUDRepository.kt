package com.tangem.data.account.repository

import arrow.core.Option
import arrow.core.Option.Companion.catch
import arrow.core.none
import arrow.core.raise.option
import com.tangem.datasource.local.datastore.RuntimeSharedStore
import com.tangem.datasource.local.userwallet.UserWalletsStore
import com.tangem.domain.account.models.AccountList
import com.tangem.domain.account.models.ArchivedAccount
import com.tangem.domain.account.repository.AccountsCRUDRepository
import com.tangem.domain.models.account.*
import com.tangem.domain.models.wallet.UserWallet
import com.tangem.domain.models.wallet.UserWalletId
import com.tangem.utils.extensions.addOrReplace
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.flow

/**
[REDACTED_AUTHOR]
 */
// TODO: [REDACTED_JIRA]
internal class DefaultAccountsCRUDRepository(
    private val runtimeStore: RuntimeSharedStore<List<AccountList>>,
    private val userWalletsStore: UserWalletsStore,
) : AccountsCRUDRepository {

    override suspend fun getAccounts(userWalletId: UserWalletId): Option<AccountList> = catch {
        runtimeStore.getSyncOrNull()
            ?.firstOrNull { it.userWallet.walletId == userWalletId }
            ?: return none()
    }

    override suspend fun getAccount(accountId: AccountId): Option<Account.CryptoPortfolio> = catch {
        runtimeStore.getSyncOrNull().orEmpty()
            .flatMap { it.accounts }
            .firstOrNull { it.accountId == accountId } as? Account.CryptoPortfolio
            ?: return none()
    }

    override suspend fun getArchivedAccount(accountId: AccountId): Option<ArchivedAccount> = option {
        createMockArchivedAccount(userWalletId = accountId.userWalletId)
    }

    override suspend fun getArchivedAccountsSync(userWalletId: UserWalletId): Option<List<ArchivedAccount>> = option {
        listOf(
            createMockArchivedAccount(userWalletId),
        )
    }

    override fun getArchivedAccounts(userWalletId: UserWalletId): Flow<List<ArchivedAccount>> {
        return flow {
            getArchivedAccountsSync(userWalletId).getOrNull().orEmpty()
        }
    }

    override suspend fun fetchArchivedAccounts(userWalletId: UserWalletId) = Unit

    override suspend fun saveAccounts(accountList: AccountList) {
        runtimeStore.update(emptyList()) {
            it.addOrReplace(accountList) { it.userWallet.walletId == accountList.userWallet.walletId }
        }
    }

    override suspend fun getTotalAccountsCount(userWalletId: UserWalletId): Int {
        val activeAccountsCount = runtimeStore.getSyncOrNull()?.size ?: 1

        return activeAccountsCount + 1
    }

    override fun getUserWallet(userWalletId: UserWalletId): UserWallet {
        return userWalletsStore.getSyncStrict(userWalletId)
    }

    private fun createMockArchivedAccount(userWalletId: UserWalletId): ArchivedAccount {
        val derivationIndex = DerivationIndex(value = 1000).getOrNull()!!

        return ArchivedAccount(
            accountId = AccountId.forCryptoPortfolio(
                userWalletId = userWalletId,
                derivationIndex = derivationIndex,
            ),
            name = AccountName("Archived Account").getOrNull()!!,
            icon = CryptoPortfolioIcon.ofDefaultCustomAccount(),
            derivationIndex = derivationIndex,
            tokensCount = 2,
            networksCount = 1,
        )
    }
}