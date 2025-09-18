package com.tangem.domain.account.usecase

import arrow.core.Either
import arrow.core.getOrElse
import arrow.core.raise.Raise
import arrow.core.raise.catch
import arrow.core.raise.either
import com.tangem.domain.account.models.AccountList
import com.tangem.domain.account.repository.AccountsCRUDRepository
import com.tangem.domain.models.account.AccountId
import com.tangem.domain.models.wallet.UserWalletId

/**
 * Use case for archiving a crypto portfolio.
 * This class provides functionality to archive a specific account within a user's crypto portfolio.
 * It ensures that the account exists and meets the necessary requirements before performing the operation.
 *
 * @property crudRepository repository for performing CRUD operations on accounts
 *
[REDACTED_AUTHOR]
 */
class ArchiveCryptoPortfolioUseCase(
    private val crudRepository: AccountsCRUDRepository,
) {

    /** Archives the specified account by its [accountId] */
    suspend operator fun invoke(accountId: AccountId): Either<Error, Unit> = either {
        val accountList = getAccountList(userWalletId = accountId.userWalletId)

        val archivingAccount = accountList.accounts
            .firstOrNull { it.accountId == accountId }
            ?: raise(Error.CriticalTechError.AccountNotFound(accountId = accountId))

        val updatedAccounts = (accountList - archivingAccount).getOrElse {
            raise(Error.CriticalTechError.AccountListRequirementsNotMet(cause = it))
        }

        saveAccounts(updatedAccounts)
    }

    private suspend fun Raise<Error>.getAccountList(userWalletId: UserWalletId): AccountList {
        return catch(
            block = { crudRepository.getAccountListSync(userWalletId = userWalletId) },
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

    /**
     * Represents possible errors that can occur during the archiving process
     */
    sealed interface Error {

        /** Error indicating that a data operation failed */
        data class DataOperationFailed(val cause: Throwable) : Error {
            override fun toString(): String = "$this: Data operation failed: ${cause.message ?: "Unknown error"}"
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

                override fun toString(): String {
                    return "${this.javaClass.simpleName}: Accounts for $userWalletId are not created"
                }
            }

            /** Error indicating that the account with [accountId] was not found */
            data class AccountNotFound(val accountId: AccountId) : CriticalTechError {
                override fun toString(): String = "${this.javaClass.simpleName}: Account with ID $accountId not found"
            }

            /**
             * Error indicating that the account list requirements were not met.
             *
             * @property cause the underlying cause of the error
             */
            data class AccountListRequirementsNotMet(val cause: AccountList.Error) : CriticalTechError {

                override fun toString(): String {
                    return "${this.javaClass.simpleName}: Account list requirements not met: $cause"
                }
            }
        }
    }
}