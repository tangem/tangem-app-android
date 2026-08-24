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
 * @property totalAccounts the backend's counter of the wallet's **crypto** accounts, archived ones included.
 * Joint accounts have their own parallel counter ([totalJointAccounts]) and are not in this one, and the special
 * accounts (payment, virtual, prediction) are injected by the app rather than counted by the backend at all — so
 * this number must never be compared against the length of [accounts]
 * @property totalJointAccounts the backend's counter of the wallet's joint accounts, archived ones included
 * (`totalJointAccounts` of `GET /accounts`). Zero for a wallet that has none, and for a list assembled without
 * joint rows at all
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
    val totalJointAccounts: Int,
    val sortType: TokensSortType,
    val groupType: TokensGroupType,
) {

    /**
     * Retrieves the main crypto portfolio account from the list of accounts.
     *
     * Always a [Account.Personal] one: a joint account is indexed in the owner key space and reports no main account,
     * so it can never answer here.
     */
    val mainAccount: Account.Personal
        get() = accounts.first { it is Account.Personal && it.isMainAccount } as Account.Personal

    /** Returns true if more accounts can be added (the maximum number of accounts has not been reached) */
    val canAddMoreCryptoAccounts: Boolean
        get() = accounts.filterIsInstance<Account.Personal>().size < MAX_CRYPTO_PORTFOLIO_ACCOUNTS_COUNT

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
        // Each counter follows the rows it counts: a joint account belongs to its own, and adding one must not
        // inflate the crypto counter — nor leave the joint one behind the list it now describes
        val isNewJointAccount = isNewAccount && other is Account.Joint

        return invoke(
            userWalletId = this.userWalletId,
            accounts = accounts,
            totalAccounts = this.totalAccounts + if (isNewAccount && other !is Account.Joint) 1 else 0,
            totalArchivedAccounts = this.totalArchivedAccounts,
            totalJointAccounts = this.totalJointAccounts + if (isNewJointAccount) 1 else 0,
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

        val wasJointAccount = isExistingAccount && other is Account.Joint

        return invoke(
            userWalletId = this.userWalletId,
            accounts = accounts,
            totalAccounts = this.totalAccounts - if (isExistingAccount && other !is Account.Joint) 1 else 0,
            totalArchivedAccounts = this.totalArchivedAccounts,
            totalJointAccounts = this.totalJointAccounts - if (wasJointAccount) 1 else 0,
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
                is Account.Personal -> account.cryptoCurrencies
                is Account.Payment -> emptyList()
                is Account.Virtual -> emptyList()
                is Account.Prediction -> emptyList()
                // A joint account is a Safe contract, not an EOA: its currencies must never reach the regular
                // send/swap/staking flows, which sign a plain transfer from the participant's own key
                is Account.Joint -> emptyList()
            }
        }
    }

    fun flattenMapCurrencies(): Map<AccountCurrencyId, CryptoCurrency> = buildMap {
        accounts
            .filterIsInstance<Account.CryptoPortfolio>()
            .forEach { account ->
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

        data object ExceedsMaxPaymentAccountsCount : Error {
            override fun toString(): String =
                "$tag: The number of payment accounts must not exceed $MAX_PAYMENT_ACCOUNTS_COUNT"
        }

        @Serializable
        data object ExceedsMaxVirtualAccountsCount : Error {
            override fun toString(): String =
                "$tag: The number of virtual accounts must not exceed $MAX_VIRTUAL_ACCOUNTS_COUNT"
        }

        @Serializable
        data object ExceedsMaxPredictionAccountsCount : Error {
            override fun toString(): String =
                "$tag: The number of prediction accounts must not exceed $MAX_PREDICTION_ACCOUNTS_COUNT"
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

        @Serializable
        data object TotalJointAccountsLessThanActive : Error {
            override fun toString(): String = "$tag: Total joint accounts cannot be less than active ones"
        }
    }

    companion object {

        const val MAX_PAYMENT_ACCOUNTS_COUNT = 1
        const val MAX_VIRTUAL_ACCOUNTS_COUNT = 1
        const val MAX_PREDICTION_ACCOUNTS_COUNT = 1
        const val MAX_CRYPTO_PORTFOLIO_ACCOUNTS_COUNT = 20
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
        @Suppress("LongParameterList")
        operator fun invoke(
            userWalletId: UserWalletId,
            accounts: List<Account>,
            totalAccounts: Int,
            totalArchivedAccounts: Int,
            totalJointAccounts: Int = 0,
            sortType: TokensSortType = TokensSortType.NONE,
            groupType: TokensGroupType = TokensGroupType.NONE,
        ): Either<Error, AccountList> = either {
            ensure(accounts.isNotEmpty()) { Error.EmptyAccountsList }

            val paymentAccounts = accounts.filterIsInstance<Account.Payment>()
            ensure(paymentAccounts.size <= MAX_PAYMENT_ACCOUNTS_COUNT) { Error.ExceedsMaxPaymentAccountsCount }

            val virtualAccounts = accounts.filterIsInstance<Account.Virtual>()
            ensure(virtualAccounts.size <= MAX_VIRTUAL_ACCOUNTS_COUNT) { Error.ExceedsMaxVirtualAccountsCount }

            val predictionAccounts = accounts.filterIsInstance<Account.Prediction>()
            ensure(predictionAccounts.size <= MAX_PREDICTION_ACCOUNTS_COUNT) {
                Error.ExceedsMaxPredictionAccountsCount
            }

            val cryptoAccounts = accounts.filterIsInstance<Account.Personal>()
            ensure(cryptoAccounts.size <= MAX_CRYPTO_PORTFOLIO_ACCOUNTS_COUNT) { Error.ExceedsMaxAccountsCount }

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

            // Joint account names come from the backend, which does not guarantee their uniqueness — two accounts
            // named "Family" from different creators are a legal response and must not invalidate the whole list
            val customNames = accounts
                .filter { it !is Account.Joint }
                .map { (it.accountName as? AccountName.Custom)?.value }
            val uniqueCustomNameCount = customNames.distinct().size

            ensure(customNames.size == uniqueCustomNameCount) {
                Error.DuplicateAccountNames
            }

            // Against the crypto accounts only, because that is what the counter counts: it comes from
            // `wallet.totalAccounts`, while joint rows are counted by `totalJointAccounts` and the special
            // accounts are added client-side. Comparing it with the whole list rejected every wallet that has a
            // joint account — the list refused to be built and the producer above retried the same failure forever
            ensure(totalAccounts >= cryptoAccounts.size) {
                Error.TotalAccountsLessThanActive
            }

            // The joint counter is policed the same way, against the joint rows alone
            ensure(totalJointAccounts >= accounts.count { it is Account.Joint }) {
                Error.TotalJointAccountsLessThanActive
            }

            AccountList(
                userWalletId = userWalletId,
                accounts = accounts,
                totalAccounts = totalAccounts,
                totalArchivedAccounts = totalArchivedAccounts,
                totalJointAccounts = totalJointAccounts,
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
            cryptoCurrencies: List<CryptoCurrency> = emptyList(),
            sortType: TokensSortType = TokensSortType.NONE,
            groupType: TokensGroupType = TokensGroupType.NONE,
        ): AccountList {
            return AccountList(
                userWalletId = userWalletId,
                accounts = listOf(
                    Account.Personal.createMainAccount(
                        userWalletId = userWalletId,
                        cryptoCurrencies = cryptoCurrencies,
                    ),
                ),
                totalAccounts = 1,
                totalArchivedAccounts = 0,
                totalJointAccounts = 0,
                sortType = sortType,
                groupType = groupType,
            )
        }

        private fun List<Account>.mainAccountsCount(): Int {
            return count { (it as? Account.CryptoPortfolio)?.isMainAccount == true }
        }
    }
}