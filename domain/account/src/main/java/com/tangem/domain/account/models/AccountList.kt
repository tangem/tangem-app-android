package com.tangem.domain.account.models

import arrow.core.Either
import arrow.core.raise.either
import arrow.core.raise.ensure
import com.tangem.domain.models.account.Account
import com.tangem.domain.models.wallet.UserWallet
import kotlinx.serialization.Serializable

/**
 * Represents a list of accounts associated with a user wallet
 *
 * @property userWallet    the user wallet associated with the account list
 * @property accounts      a set of accounts belonging to the user wallet
 * @property totalAccounts the total number of accounts
 *
[REDACTED_AUTHOR]
 */
@Serializable
data class AccountList private constructor(
    val userWallet: UserWallet,
    val accounts: Set<Account>,
    val totalAccounts: Int,
) {

    /** Retrieves the main crypto portfolio account from the list of accounts */
    val mainAccount: Account.CryptoPortfolio
        get() = accounts.first { it is Account.CryptoPortfolio && it.isMainAccount } as Account.CryptoPortfolio

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
    }

    companion object {

        /**
         * Factory method to create an `AccountList` instance.
         * Validates the input to ensure the accounts list is not empty and contains exactly one main account.
         *
         * @param userWallet    the user wallet associated with the account list
         * @param accounts      a set of accounts belonging to the user wallet
         * @param totalAccounts the total number of accounts
         */
        operator fun invoke(
            userWallet: UserWallet,
            accounts: Set<Account>,
            totalAccounts: Int,
        ): Either<Error, AccountList> = either {
            ensure(accounts.isNotEmpty()) { Error.EmptyAccountsList }

            val mainAccountsCount = accounts.mainAccountsCount()
            ensure(mainAccountsCount == 1) {
                if (mainAccountsCount == 0) {
                    Error.MainAccountNotFound
                } else {
                    Error.ExceedsMaxMainAccountsCount
                }
            }

            AccountList(userWallet = userWallet, accounts = accounts, totalAccounts = totalAccounts)
        }

        private fun Set<Account>.mainAccountsCount(): Int {
            return count { (it as? Account.CryptoPortfolio)?.isMainAccount == true }
        }
    }
}