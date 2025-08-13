package com.tangem.data.account.repository

import arrow.core.Option
import arrow.core.Option.Companion.catch
import arrow.core.none
import com.tangem.datasource.local.datastore.RuntimeSharedStore
import com.tangem.datasource.local.userwallet.UserWalletsStore
import com.tangem.domain.account.models.AccountList
import com.tangem.domain.account.repository.AccountsCRUDRepository
import com.tangem.domain.models.account.Account
import com.tangem.domain.models.account.AccountId
import com.tangem.domain.models.wallet.UserWallet
import com.tangem.domain.models.wallet.UserWalletId
import com.tangem.utils.extensions.addOrReplace

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

    override suspend fun saveAccounts(accountList: AccountList) {
        runtimeStore.update(emptyList()) {
            it.addOrReplace(accountList) { it.userWallet.walletId == accountList.userWallet.walletId }
        }
    }

    override fun getUserWallet(userWalletId: UserWalletId): UserWallet {
        return userWalletsStore.getSyncStrict(userWalletId)
    }
}