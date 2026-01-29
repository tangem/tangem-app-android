package com.tangem.domain.account.models

import arrow.core.Either
import arrow.core.raise.either
import arrow.core.raise.ensure
import com.tangem.domain.models.TokensGroupType
import com.tangem.domain.models.TokensSortType
import com.tangem.domain.models.account.Account
import com.tangem.domain.models.account.AccountId
import com.tangem.domain.models.account.AccountName
import com.tangem.domain.models.currency.CryptoCurrency
import com.tangem.domain.models.wallet.UserWalletId
import com.tangem.utils.extensions.addOrReplace
import kotlinx.serialization.Serializable

typealias AccountCurrencyId = Pair<AccountId, CryptoCurrency.ID>

/**
 * Represents a list of accounts associated with a user wallet ID.
 *
 * @property userWalletId  the user wallet id associated with the account list
 * @property accounts      a list of accounts belonging to the user wallet
 * @property totalAccounts the total number of accounts (including archived ones)
 * @property sortType      the sorting type applied to the accounts
 * @property groupType     the grouping type applied to the accounts
 *
[REDACTED_AUTHOR]
 */
@Serializable
data class AccountList private constructor(
    val userWalletId: UserWalletId,
    val accounts: List<Account>,
    val totalAccounts: Int,
    val totalArchivedAccounts: Int,
    val sortType: TokensSortType,
    val groupType: TokensGroupType,
) {

    /** Retrieves the main crypto portfolio account from the list of accounts */
    val mainAccount: Account.Crypto.Portfolio
        get() = accounts.first { it is Account.Crypto.Portfolio && it.isMainAccount } as Account.Crypto.Portfolio

    /** Returns true if more accounts can be added (the maximum number of accounts has not been reached) */
    val canAddMoreAccounts: Boolean
        get() = accounts.size < MAX_ACCOUNTS_COUNT

    /** Returns the number of active accounts in the list */
    val activeAccounts: Int
        get() = accounts.size

    /**
     * Adds an account to the account list.
     * If an account with the same identifier already exists, it will be replaced.
     * Returns a new [AccountList] instance with the updated accounts set, or a validation error if constraints are
     * violated (e.g., maximum number of accounts exceeded).
     *
     * @param other the account to add or replace
     */
    operator fun plus(other: Account): Either<Error, AccountList> {
        val isNewAccount = this.accounts.none { it.accountId == other.accountId }
        val accounts = this.accounts.addOrReplace(other) { it.accountId == other.accountId }

        return invoke(
            userWalletId = this.userWalletId,
            accounts = accounts,
            totalAccounts = this.totalAccounts + if (isNewAccount) 1 else 0,
            totalArchivedAccounts = this.totalArchivedAccounts,
            sortType = this.sortType,
            groupType = this.groupType,
        )
    }

    /**
     * Removes the specified account from the account list.
     * Returns a new [AccountList] instance with the updated accounts set, or a validation error if constraints are
     * violated (e.g., the list becomes empty).
     *
     * @param other the account to remove
     */
    operator fun minus(other: Account): Either<Error, AccountList> {
        val isExistingAccount = this.accounts.any { it.accountId == other.accountId }
        val accounts = this.accounts.toMutableList().apply {
            removeIf { it.accountId == other.accountId }
        }

        return invoke(
            userWalletId = this.userWalletId,
            accounts = accounts,
            totalAccounts = this.totalAccounts - if (isExistingAccount) 1 else 0,
            totalArchivedAccounts = this.totalArchivedAccounts,
            sortType = this.sortType,
            groupType = this.groupType,
        )
    }

    /**
     * Flattens the list of accounts to extract all crypto currencies contained within them.
     *
     * @return a list of all crypto currencies from all accounts
     */
    fun flattenCurrencies(): List<CryptoCurrency> {
        return accounts.flatMap { account ->
            when (account) {
                is Account.Crypto.Portfolio -> account.cryptoCurrencies
                is Account.Payment -> TODO("[REDACTED_JIRA]")
            }
        }
    }

    fun flattenMapCurrencies(): Map<AccountCurrencyId, CryptoCurrency> = buildMap {
        accounts.forEach { acc ->
            val account = when (acc) {
                is Account.Crypto.Portfolio -> acc
                is Account.Payment -> TODO("[REDACTED_JIRA]")
            }
            account.cryptoCurrencies.forEach { currency ->
                val key = account.accountId to currency.id
                put(key, currency)
            }
        }
    }

    /**
     * Represents possible errors that can occur when creating an `AccountList`
     */
    @Serializable
    sealed interface Error {

        val tag: String
            get() = this::class.simpleName ?: "AccountListError"

        @Serializable
        data object EmptyAccountsList : Error {
            override fun toString(): String = "$tag: The accounts list cannot be empty"
        }

        @Serializable
        data object MainAccountNotFound : Error {
            override fun toString(): String {
                return "$tag: Account list does not contain a main crypto portfolio account"
            }
        }

        @Serializable
        data object ExceedsMaxMainAccountsCount : Error {
            override fun toString(): String {
                return "$tag: There should be at most one main crypto portfolio in the account list"
            }
        }

        @Serializable
        data object ExceedsMaxAccountsCount : Error {
            override fun toString(): String = "$tag: The number of accounts must not exceed 20"
        }

        @Serializable
        data object DuplicateAccountIds : Error {
            override fun toString(): String = "$tag: Account list contains duplicate account IDs"
        }

        @Serializable
        data object DuplicateAccountNames : Error {
            override fun toString(): String = "$tag: Account list contains duplicate account names"
        }

        @Serializable
        data object TotalAccountsLessThanActive : Error {
            override fun toString(): String = "$tag: Total accounts cannot be less than active accounts"
        }
    }

    companion object {

        const val MAX_ACCOUNTS_COUNT = 20
        const val MAX_ARCHIVED_ACCOUNTS_COUNT = 1000
        private const val MAX_MAIN_ACCOUNTS_COUNT = 1

        /**
         * Factory method to create an `AccountList` instance.
         * Validates the input to ensure the accounts list is not empty and contains exactly one main account.
         *
         * @param userWalletId  the user wallet id associated with the account list
         * @param accounts      a set of accounts belonging to the user wallet
         * @param totalAccounts the total number of accounts
         */
        operator fun invoke(
            userWalletId: UserWalletId,
            accounts: List<Account>,
            totalAccounts: Int,
            totalArchivedAccounts: Int,
            sortType: TokensSortType = TokensSortType.NONE,
            groupType: TokensGroupType = TokensGroupType.NONE,
        ): Either<Error, AccountList> = either {
            ensure(accounts.isNotEmpty()) { Error.EmptyAccountsList }

            ensure(accounts.size <= MAX_ACCOUNTS_COUNT) { Error.ExceedsMaxAccountsCount }

            val mainAccountsCount = accounts.mainAccountsCount()
            ensure(mainAccountsCount == MAX_MAIN_ACCOUNTS_COUNT) {
                if (mainAccountsCount == 0) {
                    Error.MainAccountNotFound
                } else {
                    Error.ExceedsMaxMainAccountsCount
                }
            }

            val uniqueAccountIdsCount = accounts.map { it.accountId.value }.distinct().size
            ensure(accounts.size == uniqueAccountIdsCount) { Error.DuplicateAccountIds }

            val customNames = accounts.map { (it.accountName as? AccountName.Custom)?.value }
            val uniqueCustomNameCount = customNames.distinct().size

            ensure(customNames.size == uniqueCustomNameCount) {
                Error.DuplicateAccountNames
            }

            ensure(totalAccounts >= accounts.size) {
                Error.TotalAccountsLessThanActive
            }

            AccountList(
                userWalletId = userWalletId,
                accounts = accounts,
                totalAccounts = totalAccounts,
                totalArchivedAccounts = totalArchivedAccounts,
                sortType = sortType,
                groupType = groupType,
            )
        }

        /**
         * Factory method to create an empty [AccountList] with a main crypto portfolio account
         *
         * @param userWalletId the user wallet id associated with the account list
         */
        fun empty(
            userWalletId: UserWalletId,
            cryptoCurrencies: Set<CryptoCurrency> = emptySet(),
            sortType: TokensSortType = TokensSortType.NONE,
            groupType: TokensGroupType = TokensGroupType.NONE,
        ): AccountList {
            return AccountList(
                userWalletId = userWalletId,
                accounts = listOf(
                    Account.Crypto.Portfolio.createMainAccount(
                        userWalletId = userWalletId,
                        cryptoCurrencies = cryptoCurrencies,
                    ),
                ),
                totalAccounts = 1,
                totalArchivedAccounts = 0,
                sortType = sortType,
                groupType = groupType,
            )
        }

        private fun List<Account>.mainAccountsCount(): Int {
            return count { (it as? Account.Crypto.Portfolio)?.isMainAccount == true }
        }
    }
}