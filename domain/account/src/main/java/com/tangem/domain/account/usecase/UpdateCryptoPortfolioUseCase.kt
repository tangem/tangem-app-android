package com.tangem.domain.account.usecase

import arrow.core.Either
import arrow.core.getOrElse
import arrow.core.raise.*
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
        validate(accountName, icon)

        val accountList = getAccountList(userWalletId = accountId.userWalletId)

        val account = accountList.accounts
            .firstOrNull { it.accountId == accountId } as? Account.CryptoPortfolio
            ?: raise(Error.DataOperationFailed(message = "Account not found: $accountId"))

        val updatedAccount = account
            .setName(name = accountName)
            .setIcon(icon = icon)

        val updatedAccounts = withError({ Error.AccountListRequirementsNotMet(it) }) {
            if (accountName != null) {
                checkDefaultName(accountList, accountName)
            }

            (accountList + updatedAccount).bind()
        }

        saveAccounts(updatedAccounts)

        updatedAccount
    }

    private fun Raise<Error>.validate(accountName: AccountName?, icon: CryptoPortfolioIcon?) {
        ensure(accountName != null || icon != null) { Error.NothingToUpdate }
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

    private fun Account.CryptoPortfolio.setName(name: AccountName?): Account.CryptoPortfolio {
        return if (name != null) this.copy(accountName = name) else this
    }

    private fun Account.CryptoPortfolio.setIcon(icon: CryptoPortfolioIcon?): Account.CryptoPortfolio {
        return if (icon != null) this.copy(icon = icon) else this
    }

    private fun Raise<AccountList.Error>.checkDefaultName(accountList: AccountList, accountName: AccountName) {
        withError({ AccountList.Error.DuplicateAccountNames }) {
            Either.Companion.catch { crudRepository.checkDefaultAccountName(accountList, accountName) }.bind()
        }
    }

    /**
     * Represents possible errors that can occur during the update operation
     */
    sealed interface Error {

        /** Error indicating that there is nothing to update */
        data object NothingToUpdate : Error {
            override fun toString(): String = "Nothing to update: both account name and icon are null"
        }

        data class AccountListRequirementsNotMet(val cause: AccountList.Error) : Error {
            override fun toString(): String = "Account list requirements not met: $cause"
        }

        /** Error indicating that a data operation failed */
        data class DataOperationFailed(val cause: Throwable) : Error {

            constructor(message: String) : this(cause = IllegalStateException(message))
        }
    }
}