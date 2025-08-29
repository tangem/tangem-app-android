package com.tangem.data.account.repository

import arrow.core.Option
import arrow.core.raise.option
import arrow.core.toOption
import com.tangem.data.account.converter.AccountConverterFactoryContainer
import com.tangem.data.account.converter.ArchivedAccountConverter
import com.tangem.data.account.converter.SaveWalletAccountsResponseConverter
import com.tangem.data.account.store.AccountsResponseStore
import com.tangem.data.account.store.AccountsResponseStoreFactory
import com.tangem.data.account.store.ArchivedAccountsStore
import com.tangem.data.account.store.ArchivedAccountsStoreFactory
import com.tangem.datasource.api.common.response.getOrThrow
import com.tangem.datasource.api.tangemTech.TangemTechApi
import com.tangem.datasource.api.tangemTech.models.account.GetWalletAccountsResponse
import com.tangem.datasource.local.userwallet.UserWalletsStore
import com.tangem.domain.account.models.AccountList
import com.tangem.domain.account.models.ArchivedAccount
import com.tangem.domain.account.repository.AccountsCRUDRepository
import com.tangem.domain.models.account.Account
import com.tangem.domain.models.account.AccountId
import com.tangem.domain.models.wallet.UserWallet
import com.tangem.domain.models.wallet.UserWalletId
import com.tangem.utils.coroutines.CoroutineDispatcherProvider
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.firstOrNull
import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.sync.withLock
import kotlinx.coroutines.withContext

/**
[REDACTED_AUTHOR]
 */
internal class DefaultAccountsCRUDRepository(
    private val tangemTechApi: TangemTechApi,
    private val accountsResponseStoreFactory: AccountsResponseStoreFactory,
    private val archivedAccountsStoreFactory: ArchivedAccountsStoreFactory,
    private val userWalletsStore: UserWalletsStore,
    private val convertersContainer: AccountConverterFactoryContainer,
    private val dispatchers: CoroutineDispatcherProvider,
) : AccountsCRUDRepository {

    private val saveAccountsMutex = Mutex()

    override suspend fun getAccountListSync(userWalletId: UserWalletId): Option<AccountList> = option {
        val accountListResponse = getAccountsResponseSync(userWalletId = userWalletId)

        ensureNotNull(accountListResponse)

        val converter = convertersContainer.createAccountListConverter(userWalletId = userWalletId)
        converter.convert(value = accountListResponse)
    }

    override suspend fun getAccountSync(accountId: AccountId): Option<Account.CryptoPortfolio> = option {
        val userWalletId = accountId.userWalletId

        val accountResponse = getAccountsResponseSync(userWalletId = userWalletId)
            ?.accounts?.firstOrNull { it.id == accountId.value }

        ensureNotNull(accountResponse)

        val converter = convertersContainer.createCryptoPortfolioConverter(userWalletId = userWalletId)
        converter.convert(value = accountResponse)
    }

    override suspend fun getArchivedAccountSync(accountId: AccountId): Option<ArchivedAccount> {
        val store = getArchivedAccountsStore(userWalletId = accountId.userWalletId)

        return store.getSyncOrNull()
            ?.firstOrNull { it.accountId == accountId }
            .toOption()
    }

    override suspend fun getArchivedAccountListSync(userWalletId: UserWalletId): Option<List<ArchivedAccount>> {
        val store = getArchivedAccountsStore(userWalletId = userWalletId)

        return store.getSyncOrNull().toOption()
    }

    override fun getArchivedAccounts(userWalletId: UserWalletId): Flow<List<ArchivedAccount>> {
        val store = getArchivedAccountsStore(userWalletId = userWalletId)

        return store.get()
    }

    override suspend fun fetchArchivedAccounts(userWalletId: UserWalletId) {
        val response = withContext(dispatchers.io) {
            tangemTechApi.getWalletArchivedAccounts(walletId = userWalletId.stringValue).getOrThrow()
        }

        val store = getArchivedAccountsStore(userWalletId = userWalletId)
        val converter = ArchivedAccountConverter(userWalletId = userWalletId)

        val archivedAccounts = converter.convertList(input = response.accounts)

        store.store(value = archivedAccounts)
    }

    override suspend fun saveAccounts(accountList: AccountList) {
        saveAccountsMutex.withLock {
            val store = getAccountsResponseStore(userWalletId = accountList.userWallet.walletId)

            val version = store.data.firstOrNull()?.wallet?.version ?: 0
            val body = SaveWalletAccountsResponseConverter.convert(value = accountList)

            withContext(dispatchers.io) {
                tangemTechApi.saveWalletAccounts(
                    walletId = accountList.userWallet.walletId.stringValue,
                    ifMatch = version.toString(),
                    body = body,
                )
                    .getOrThrow()
            }

            val converter = convertersContainer.getWalletAccountsResponseCF.create(
                userWallet = accountList.userWallet,
                version = version,
            )

            val accountsResponse = converter.convert(value = accountList)

            store.updateData { accountsResponse }
        }
    }

    override suspend fun getTotalAccountsCount(userWalletId: UserWalletId): Option<Int> = option {
        val accountListResponse = getAccountsResponseSync(userWalletId = userWalletId)

        ensureNotNull(accountListResponse)

        return accountListResponse.wallet.totalAccounts.toOption()
    }

    override fun getUserWallet(userWalletId: UserWalletId): UserWallet {
        return userWalletsStore.getSyncStrict(userWalletId)
    }

    private suspend fun getAccountsResponseSync(userWalletId: UserWalletId): GetWalletAccountsResponse? {
        val store = getAccountsResponseStore(userWalletId = userWalletId)
        return store.data.firstOrNull()
    }

    private fun getAccountsResponseStore(userWalletId: UserWalletId): AccountsResponseStore {
        return accountsResponseStoreFactory.create(userWalletId = userWalletId)
    }

    private fun getArchivedAccountsStore(userWalletId: UserWalletId): ArchivedAccountsStore {
        return archivedAccountsStoreFactory.create(userWalletId)
    }
}