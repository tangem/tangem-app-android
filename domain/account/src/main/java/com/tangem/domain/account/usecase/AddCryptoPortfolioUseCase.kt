package com.tangem.domain.account.usecase

import arrow.core.Either
import arrow.core.Option
import arrow.core.getOrElse
import arrow.core.raise.Raise
import arrow.core.raise.catch
import arrow.core.raise.either
import com.tangem.domain.account.models.AccountList
import com.tangem.domain.account.repository.AccountsCRUDRepository
import com.tangem.domain.models.TokensGroupType
import com.tangem.domain.models.TokensSortType
import com.tangem.domain.models.account.Account
import com.tangem.domain.models.account.AccountId
import com.tangem.domain.models.account.AccountName
import com.tangem.domain.models.account.CryptoPortfolioIcon
import com.tangem.domain.models.wallet.UserWalletId
import java.util.UUID

/**
 * Use case for adding a new crypto portfolio account
 *
 * @property crudRepository the repository used for performing CRUD operations on accounts
 *
[REDACTED_AUTHOR]
 */
class AddCryptoPortfolioUseCase(
    private val crudRepository: AccountsCRUDRepository,
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
        derivationIndex: Int,
    ): Either<Error, Account.CryptoPortfolio> = either {
        val newAccount = createAccount(userWalletId, accountName, icon, derivationIndex)

        val accountList = getAccountList(userWalletId = userWalletId).getOrElse {
            createNewAccountList(userWalletId = userWalletId)
        }

        val updatedAccounts = (accountList + newAccount)
            .getOrElse { raise(Error.AccountListRequirementsNotMet(it)) }

        saveAccounts(updatedAccounts)

        newAccount
    }

    private fun Raise<Error>.createAccount(
        userWalletId: UserWalletId,
        accountName: AccountName,
        icon: CryptoPortfolioIcon,
        derivationIndex: Int,
    ): Account.CryptoPortfolio {
        // TODO: [REDACTED_JIRA]
        return Account.CryptoPortfolio(
            accountId = AccountId(userWalletId = userWalletId, value = UUID.randomUUID().toString()),
            accountName = accountName,
            accountIcon = icon,
            derivationIndex = derivationIndex,
            isArchived = false,
            cryptoCurrencyList = Account.CryptoPortfolio.CryptoCurrencyList(
                currencies = emptySet(),
                sortType = TokensSortType.NONE,
                groupType = TokensGroupType.NONE,
            ),
        )
            .getOrElse { raise(Error.AccountCreation(it)) }
    }

    private suspend fun Raise<Error>.getAccountList(userWalletId: UserWalletId): Option<AccountList> {
        return catch(
            block = { crudRepository.getAccounts(userWalletId = userWalletId) },
            catch = { raise(Error.DataOperationFailed(cause = it)) },
        )
    }

    private fun Raise<Error>.createNewAccountList(userWalletId: UserWalletId): AccountList {
        val userWallet = catch(
            block = { crudRepository.getUserWallet(userWalletId = userWalletId) },
            catch = { raise(Error.DataOperationFailed(cause = it)) },
        )

        return AccountList.createEmpty(userWallet = userWallet)
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
         * Error indicating that the account creation failed
         *
         * @property cause the underlying cause of the failure
         */
        data class AccountCreation(val cause: Account.CryptoPortfolio.Error) : Error

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
            override fun toString(): String = "Data operation failed: ${cause.message ?: "Unknown error"}"
        }
    }
}