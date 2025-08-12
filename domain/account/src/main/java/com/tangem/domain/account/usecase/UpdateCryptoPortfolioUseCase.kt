package com.tangem.domain.account.usecase

import arrow.core.Either
import arrow.core.getOrElse
import arrow.core.raise.Raise
import arrow.core.raise.catch
import arrow.core.raise.either
import arrow.core.raise.ensure
import com.tangem.domain.account.models.AccountList
import com.tangem.domain.account.repository.AccountsCRUDRepository
import com.tangem.domain.models.account.Account
import com.tangem.domain.models.account.AccountId
import com.tangem.domain.models.account.AccountName
import com.tangem.domain.models.account.CryptoPortfolioIcon
import com.tangem.domain.models.wallet.UserWalletId

/**
 * Use case for updating a crypto portfolio account.
 *
 * @property crudRepository the repository used for performing CRUD operations on accounts
 *
[REDACTED_AUTHOR]
 */
class UpdateCryptoPortfolioUseCase(
    private val crudRepository: AccountsCRUDRepository,
) {

    /**
     * Updates a crypto portfolio account with the provided name and/or icon
     *
     * @param accountId   the unique identifier of the account to update
     * @param accountName the new name for the account (optional)
     * @param icon        the new icon for the account (optional)
     * @return an [Either] containing the updated [Account.CryptoPortfolio] on success, or an [Error] on failure
     */
    suspend operator fun invoke(
        accountId: AccountId,
        accountName: AccountName? = null,
        icon: CryptoPortfolioIcon? = null,
    ): Either<Error, Account.CryptoPortfolio> = either {
        ensure(accountName != null || icon != null) { Error.NothingToUpdate }

        val accountList = getAccountList(userWalletId = accountId.userWalletId)

        val account = accountList.accounts
            .firstOrNull { it.accountId == accountId } as? Account.CryptoPortfolio
            ?: raise(Error.CriticalTechError.AccountNotFound(accountId = accountId))

        val updatedAccount = account
            .setName(name = accountName)
            .setIcon(icon = icon)

        val updatedAccounts = (accountList + updatedAccount).getOrElse {
            raise(Error.CriticalTechError.AccountListRequirementsNotMet(it))
        }

        saveAccounts(updatedAccounts)

        updatedAccount
    }

    private suspend fun Raise<Error>.getAccountList(userWalletId: UserWalletId): AccountList {
        return catch(
            block = { crudRepository.getAccounts(userWalletId = userWalletId) },
            catch = { raise(Error.DataOperationFailed(cause = it)) },
        )
            .getOrElse { raise(Error.CriticalTechError.AccountsNotCreated(userWalletId = userWalletId)) }
    }

    private suspend fun Raise<Error>.saveAccounts(accountList: AccountList) {
        catch(
            block = { crudRepository.saveAccounts(accountList) },
            catch = { raise(Error.DataOperationFailed(cause = it)) },
        )
    }

    private fun Account.CryptoPortfolio.setName(name: AccountName?): Account.CryptoPortfolio {
        return if (name != null) this.copy(accountName = name) else this
    }

    private fun Account.CryptoPortfolio.setIcon(icon: CryptoPortfolioIcon?): Account.CryptoPortfolio {
        return if (icon != null) this.copy(accountIcon = icon) else this
    }

    /**
     * Represents possible errors that can occur during the update operation
     */
    sealed interface Error {

        /** Error indicating that there is nothing to update */
        data object NothingToUpdate : Error {
            override fun toString(): String = "Nothing to update: both account name and icon are null"
        }

        /** Error indicating that a data operation failed */
        data class DataOperationFailed(val cause: Throwable) : Error {
            override fun toString(): String = "Data operation failed: ${cause.message ?: "Unknown error"}"
        }

        /**
         * Represents critical technical errors that can occur during the update operation.
         * These errors are a consequence of an inconsistent state.
         */
        sealed interface CriticalTechError : Error {

            /**

             *
             * @property userWalletId the unique identifier of the user wallet
             */
            data class AccountsNotCreated(val userWalletId: UserWalletId) : CriticalTechError {
                override fun toString(): String = "Accounts for $userWalletId are not created"
            }

            /** Error indicating that the account with [accountId] was not found */
            data class AccountNotFound(val accountId: AccountId) : CriticalTechError {
                override fun toString(): String = "Account with ID $accountId not found"
            }

            /**
             * Error indicating that the account list requirements were not met.
             *
             * @property cause the underlying cause of the error
             */
            data class AccountListRequirementsNotMet(val cause: AccountList.Error) : CriticalTechError {
                override fun toString(): String = "Account list requirements not met: $cause"
            }
        }
    }
}