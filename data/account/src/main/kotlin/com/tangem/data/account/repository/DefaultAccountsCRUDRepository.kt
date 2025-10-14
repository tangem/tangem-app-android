package com.tangem.data.account.repository

import arrow.core.Option
import arrow.core.raise.option
import arrow.core.toOption
import com.tangem.data.account.converter.AccountConverterFactoryContainer
import com.tangem.data.account.converter.ArchivedAccountConverter
import com.tangem.data.account.store.AccountsResponseStore
import com.tangem.data.account.store.AccountsResponseStoreFactory
import com.tangem.data.account.store.ArchivedAccountsStore
import com.tangem.data.account.store.ArchivedAccountsStoreFactory
import com.tangem.data.account.utils.toUserTokensResponse
import com.tangem.data.common.account.WalletAccountsSaver
import com.tangem.data.common.cache.etag.ETagsStore
import com.tangem.data.common.currency.UserTokensSaver
import com.tangem.datasource.api.common.response.getOrThrow
import com.tangem.datasource.api.tangemTech.TangemTechApi
import com.tangem.datasource.api.tangemTech.models.account.GetWalletAccountsResponse
import com.tangem.datasource.local.userwallet.UserWalletsStore
import com.tangem.datasource.utils.getSyncOrNull
import com.tangem.domain.account.models.AccountList
import com.tangem.domain.account.models.ArchivedAccount
import com.tangem.domain.account.repository.AccountsCRUDRepository
import com.tangem.domain.models.account.Account
import com.tangem.domain.models.account.AccountId
import com.tangem.domain.models.wallet.UserWallet
import com.tangem.domain.models.wallet.UserWalletId
import com.tangem.utils.coroutines.CoroutineDispatcherProvider
import com.tangem.utils.extensions.replaceBy
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.withContext
import timber.log.Timber

/**
[REDACTED_AUTHOR]
 */
@Suppress("LongParameterList")
internal class DefaultAccountsCRUDRepository(
    private val tangemTechApi: TangemTechApi,
    private val walletAccountsSaver: WalletAccountsSaver,
    private val accountsResponseStoreFactory: AccountsResponseStoreFactory,
    private val archivedAccountsStoreFactory: ArchivedAccountsStoreFactory,
    private val userWalletsStore: UserWalletsStore,
    private val userTokensSaver: UserTokensSaver,
    private val eTagsStore: ETagsStore,
    private val convertersContainer: AccountConverterFactoryContainer,
    private val dispatchers: CoroutineDispatcherProvider,
) : AccountsCRUDRepository {

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
            tangemTechApi.getWalletArchivedAccounts(
                walletId = userWalletId.stringValue,
                eTag = getETag(userWalletId),
            ).getOrThrow()
        }

        val store = getArchivedAccountsStore(userWalletId = userWalletId)
        val converter = ArchivedAccountConverter(userWalletId = userWalletId)

        val archivedAccounts = converter.convertList(input = response.accounts)

        store.store(value = archivedAccounts)
    }

    override suspend fun saveAccounts(accountList: AccountList) {
        val converter = convertersContainer.createCryptoPortfolioConverter(userWalletId = accountList.userWalletId)

        val accountDTOs = converter.convertListBack(
            input = accountList.accounts.filterIsInstance<Account.CryptoPortfolio>(),
        )

        val syncedResponse = walletAccountsSaver.push(userWalletId = accountList.userWalletId, accounts = accountDTOs)
        if (syncedResponse != null) {
            walletAccountsSaver.store(userWalletId = accountList.userWalletId, response = syncedResponse)
        }
    }

    override suspend fun saveAccount(account: Account.CryptoPortfolio) {
        val store = getAccountsResponseStore(userWalletId = account.userWalletId)

        val converter = convertersContainer.createCryptoPortfolioConverter(userWalletId = account.userWalletId)
        val newAccountDTO = converter.convertBack(value = account)

        store.updateData { response ->
            response ?: return@updateData response

            response.copy(
                accounts = response.accounts.toMutableList().apply {
                    replaceBy(newAccountDTO) { it.id == newAccountDTO.id }
                },
            )
        }
    }

    override suspend fun syncTokens(userWalletId: UserWalletId) {
        val response = getAccountsResponseSync(userWalletId = userWalletId)

        if (response == null) {
            Timber.e("Can't sync tokens. No accounts response found for wallet: $userWalletId")
            return
        }

        userTokensSaver.push(userWalletId = userWalletId, response = response.toUserTokensResponse())
    }

    override suspend fun getTotalAccountsCountSync(userWalletId: UserWalletId): Option<Int> = option {
        val accountListResponse = getAccountsResponseSync(userWalletId = userWalletId)

        ensureNotNull(accountListResponse)

        return accountListResponse.wallet.totalAccounts.toOption()
    }

    override fun getTotalAccountsCount(userWalletId: UserWalletId): Flow<Option<Int>> {
        return getAccountsResponseStore(userWalletId = userWalletId).data
            .map { it?.wallet?.totalAccounts.toOption() }
    }

    override fun getUserWallet(userWalletId: UserWalletId): UserWallet {
        return userWalletsStore.getSyncStrict(userWalletId)
    }

    override fun getUserWallets(): Flow<List<UserWallet>> = userWalletsStore.userWallets

    override fun getUserWalletsSync(): List<UserWallet> = userWalletsStore.userWalletsSync

    private suspend fun getETag(userWalletId: UserWalletId): String? {
        return eTagsStore.getSyncOrNull(userWalletId = userWalletId, key = ETagsStore.Key.WalletAccounts)
    }

    private suspend fun getAccountsResponseSync(userWalletId: UserWalletId): GetWalletAccountsResponse? {
        val store = getAccountsResponseStore(userWalletId = userWalletId)
        return store.getSyncOrNull()
    }

    private fun getAccountsResponseStore(userWalletId: UserWalletId): AccountsResponseStore {
        return accountsResponseStoreFactory.create(userWalletId = userWalletId)
    }

    private fun getArchivedAccountsStore(userWalletId: UserWalletId): ArchivedAccountsStore {
        return archivedAccountsStoreFactory.create(userWalletId)
    }
}