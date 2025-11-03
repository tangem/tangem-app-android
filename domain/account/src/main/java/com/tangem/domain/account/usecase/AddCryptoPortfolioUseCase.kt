package com.tangem.domain.account.usecase

import arrow.core.Either
import arrow.core.getOrElse
import arrow.core.raise.Raise
import arrow.core.raise.catch
import arrow.core.raise.either
import com.tangem.domain.account.fetcher.SingleAccountListFetcher
import com.tangem.domain.account.models.AccountList
import com.tangem.domain.account.repository.AccountsCRUDRepository
import com.tangem.domain.account.tokens.MainAccountTokensMigration
import com.tangem.domain.models.account.*
import com.tangem.domain.models.wallet.UserWalletId

/**
 * Use case for adding a new crypto portfolio account
 *
 * @property crudRepository the repository used for performing CRUD operations on accounts
 * @property singleAccountListFetcher fetches the list of accounts for a single user wallet
 * @property mainAccountTokensMigration handles the migration of tokens from the main account to the new
 *
[REDACTED_AUTHOR]
 */
class AddCryptoPortfolioUseCase(
    private val crudRepository: AccountsCRUDRepository,
    private val singleAccountListFetcher: SingleAccountListFetcher,
    private val mainAccountTokensMigration: MainAccountTokensMigration,
) {

    /**
     * Adds a new crypto portfolio account to the repository
     *
     * @param userWalletId    the unique identifier of the user wallet
     * @param accountName     the name of the new account
     * @param icon            the icon representing the new account
     * @param derivationIndex the derivation index for the account
     *

     */
    suspend operator fun invoke(
        userWalletId: UserWalletId,
        accountName: AccountName,
        icon: CryptoPortfolioIcon,
        derivationIndex: DerivationIndex,
    ): Either<Error, Account.CryptoPortfolio> = either {
        fetchAccountList(userWalletId)

        val accountList = getAccountList(userWalletId = userWalletId)

        val newAccount = createAccount(userWalletId, accountName, icon, derivationIndex)

        val updatedAccounts = (accountList + newAccount).getOrElse {
            raise(Error.AccountListRequirementsNotMet(it))
        }

        saveAccounts(accountList = updatedAccounts)

        mainAccountTokensMigration.migrate(userWalletId, derivationIndex)

        newAccount
    }

    private suspend fun Raise<Error>.fetchAccountList(userWalletId: UserWalletId) {
        singleAccountListFetcher(params = SingleAccountListFetcher.Params(userWalletId)).onLeft {
            raise(Error.DataOperationFailed(cause = it))
        }
    }

    private fun createAccount(
        userWalletId: UserWalletId,
        accountName: AccountName,
        icon: CryptoPortfolioIcon,
        derivationIndex: DerivationIndex,
    ): Account.CryptoPortfolio {
        return Account.CryptoPortfolio(
            accountId = AccountId.forCryptoPortfolio(userWalletId = userWalletId, derivationIndex = derivationIndex),
            accountName = accountName,
            icon = icon,
            derivationIndex = derivationIndex,
            cryptoCurrencies = emptySet(),
        )
    }

    private suspend fun Raise<Error>.getAccountList(userWalletId: UserWalletId): AccountList {
        return catch(
            block = { crudRepository.getAccountListSync(userWalletId = userWalletId) },
            catch = { raise(Error.DataOperationFailed(cause = it)) },
        )
            .getOrElse {
                raise(Error.DataOperationFailed(message = "Account list not found for wallet $userWalletId"))
            }
    }

    private suspend fun Raise<Error>.saveAccounts(accountList: AccountList) {
        catch(
            block = { crudRepository.saveAccounts(accountList) },
            catch = { raise(Error.DataOperationFailed(cause = it)) },
        )
    }

    /**
     * Represents possible errors that can occur during the add operation
     */
    sealed interface Error {

        /**
         * Error indicating that the account list requirements were not met.
         *
         * @property cause the underlying cause of the error
         */
        data class AccountListRequirementsNotMet(val cause: AccountList.Error) : Error {
            override fun toString(): String = "Account list requirements not met: $cause"
        }

        /** Error indicating that a data operation failed */
        data class DataOperationFailed(val cause: Throwable) : Error {

            constructor(message: String) : this(cause = IllegalStateException(message))
        }
    }
}