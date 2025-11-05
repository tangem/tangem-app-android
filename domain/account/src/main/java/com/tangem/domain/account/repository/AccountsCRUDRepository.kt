package com.tangem.domain.account.repository

import arrow.core.Option
import com.tangem.domain.account.models.AccountList
import com.tangem.domain.account.models.ArchivedAccount
import com.tangem.domain.models.account.Account
import com.tangem.domain.models.account.AccountId
import com.tangem.domain.models.account.AccountName
import com.tangem.domain.models.wallet.UserWallet
import com.tangem.domain.models.wallet.UserWalletId
import kotlinx.coroutines.flow.Flow

/**
 * Repository interface for performing CRUD operations on accounts
 *
[REDACTED_AUTHOR]
 */
interface AccountsCRUDRepository {

    /**
     * Retrieves a list of accounts associated with a specific user wallet
     *
     * @param userWalletId the unique identifier of the user wallet
     * @return an [Option] containing the [AccountList] if found, or `Option.None` if not
     */
    suspend fun getAccountListSync(userWalletId: UserWalletId): Option<AccountList>

    /**
     * Retrieves a specific account by its unique identifier
     *
     * @param accountId the unique identifier of the account
     * @return an [Option] containing the [Account.CryptoPortfolio] if found, or `Option.None` if not
     */
    suspend fun getAccountSync(accountId: AccountId): Option<Account.CryptoPortfolio>

    /**
     * Retrieves a archived account by its unique identifier
     *
     * @param accountId the unique identifier of the account
     */
    suspend fun getArchivedAccountSync(accountId: AccountId): Option<ArchivedAccount>

    /**
     * Retrieves a list of archived accounts associated with a specific user wallet
     *
     * @param userWalletId the unique identifier of the user wallet
     * @return an [Option] containing a list of [ArchivedAccount] if found, or `Option.None` if not
     */
    suspend fun getArchivedAccountListSync(userWalletId: UserWalletId): Option<List<ArchivedAccount>>

    /**
     * Provides a flow of archived accounts associated with a specific user wallet
     *
     * @param userWalletId the unique identifier of the user wallet
     */
    fun getArchivedAccounts(userWalletId: UserWalletId): Flow<List<ArchivedAccount>>

    /**
     * Fetches archived accounts for a specific user wallet and updates the repository
     *
     * @param userWalletId the unique identifier of the user wallet
     */
    suspend fun fetchArchivedAccounts(userWalletId: UserWalletId)

    /**
     * Saves a list of accounts to the repository
     *
     * @param accountList the list of accounts to be saved.
     */
    suspend fun saveAccountsLocally(accountList: AccountList)

    /**
     * Saves a list of accounts to the repository
     *
     * @param accountList the list of accounts to be saved.
     */
    suspend fun saveAccounts(accountList: AccountList)

    /**
     * Save account
     *
     * @param account account to be saved
     */
    suspend fun saveAccount(account: Account.CryptoPortfolio)

    /** Synchronizes tokens for a specific [userWalletId] with remote data source */
    suspend fun syncTokens(userWalletId: UserWalletId)

    /**
     * Retrieves the total count of active accounts associated with a specific user wallet excluding archived accounts
     *
     * @param userWalletId the unique identifier of the user wallet
     */
    suspend fun getTotalAccountsCountSync(userWalletId: UserWalletId): Option<Int>

    /**
     * Retrieves the total count of active accounts associated with a specific user wallet excluding archived accounts
     *
     * @param userWalletId the unique identifier of the user wallet
     */
    suspend fun getTotalActiveAccountsCountSync(userWalletId: UserWalletId): Option<Int>

    /**
     * Provides a flow of the total count of active accounts associated with a specific user wallet excluding
     * archived accounts
     *
     * @param userWalletId the unique identifier of the user wallet
     */
    fun getTotalActiveAccountsCount(userWalletId: UserWalletId): Flow<Option<Int>>

    /**
     * Retrieves a user wallet by its unique identifier
     *
     * @param userWalletId the unique identifier of the user wallet
     * @return the [UserWallet] associated with the given identifier
     */
    fun getUserWallet(userWalletId: UserWalletId): UserWallet

    /** Provides a flow of all user wallets */
    fun getUserWallets(): Flow<List<UserWallet>>

    /** Synchronously retrieves all user wallets */
    fun getUserWalletsSync(): List<UserWallet>

    /** Checks if the provided account name is the default name within the given account list
     *
     * @param accountList the list of accounts to check against
     * @param accountName the account name to be checked
     * @throws IllegalArgumentException if the account name matches the default name
     */
    fun checkDefaultAccountName(accountList: AccountList, accountName: AccountName)
}