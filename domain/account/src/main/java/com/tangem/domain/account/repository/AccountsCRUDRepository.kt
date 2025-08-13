package com.tangem.domain.account.repository

import arrow.core.Option
import com.tangem.domain.account.models.AccountList
import com.tangem.domain.account.models.ArchivedAccount
import com.tangem.domain.models.account.Account
import com.tangem.domain.models.account.AccountId
import com.tangem.domain.models.wallet.UserWallet
import com.tangem.domain.models.wallet.UserWalletId

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
    suspend fun getAccounts(userWalletId: UserWalletId): Option<AccountList>

    /**
     * Retrieves a specific account by its unique identifier
     *
     * @param accountId the unique identifier of the account
     * @return an [Option] containing the [Account.CryptoPortfolio] if found, or `Option.None` if not
     */
    suspend fun getAccount(accountId: AccountId): Option<Account.CryptoPortfolio>

    /**
     * Retrieves a archived account by its unique identifier
     *
     * @param accountId the unique identifier of the account
     */
    suspend fun getArchivedAccount(accountId: AccountId): Option<ArchivedAccount>

    /**
     * Saves a list of accounts to the repository
     *
     * @param accountList the list of accounts to be saved.
     */
    suspend fun saveAccounts(accountList: AccountList)

    /**
     * Retrieves the total count of accounts associated with a specific user wallet including archived accounts
     *
     * @param userWalletId the unique identifier of the user wallet
     */
    suspend fun getTotalAccountsCount(userWalletId: UserWalletId): Int

    /**
     * Retrieves a user wallet by its unique identifier
     *
     * @param userWalletId the unique identifier of the user wallet
     * @return the [UserWallet] associated with the given identifier
     */
    fun getUserWallet(userWalletId: UserWalletId): UserWallet
}