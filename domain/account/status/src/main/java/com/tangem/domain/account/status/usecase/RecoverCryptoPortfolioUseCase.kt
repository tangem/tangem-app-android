package com.tangem.domain.account.status.usecase

import arrow.core.Either
import arrow.core.getOrElse
import arrow.core.merge
import arrow.core.raise.Raise
import arrow.core.raise.catch
import arrow.core.raise.either
import arrow.core.raise.ensure
import com.tangem.domain.account.models.AccountList
import com.tangem.domain.account.models.ArchivedAccount
import com.tangem.domain.account.repository.AccountsCRUDRepository
import com.tangem.domain.account.status.utils.AccountNameIndexer
import com.tangem.domain.account.status.utils.CryptoCurrencyBalanceFetcher
import com.tangem.domain.account.tokens.MainAccountTokensMigration
import com.tangem.domain.models.account.Account
import com.tangem.domain.models.account.AccountId
import com.tangem.domain.models.account.AccountName
import com.tangem.domain.models.wallet.UserWalletId

/**
 * Use case for recovering a crypto portfolio account from archived accounts
 *
 * @property crudRepository repository for performing CRUD operations on accounts
 * @property mainAccountTokensMigration handles the migration of tokens from the main account to the recovered account
 *
[REDACTED_AUTHOR]
 */
class RecoverCryptoPortfolioUseCase(
    private val crudRepository: AccountsCRUDRepository,
    private val mainAccountTokensMigration: MainAccountTokensMigration,
    private val cryptoCurrencyBalanceFetcher: CryptoCurrencyBalanceFetcher,
) {

    /**
     * Recovers a crypto portfolio account by moving it from archived accounts to active accounts
     *
     * @param accountId the unique identifier of the account to recover
     */
    suspend operator fun invoke(accountId: AccountId): Either<Error, Account.CryptoPortfolio> = either {
        val accountList = getAccountList(userWalletId = accountId.userWalletId)

        ensure(accountList.canAddMoreAccounts) {
            raise(Error.AccountListRequirementsNotMet(cause = AccountList.Error.ExceedsMaxAccountsCount))
        }

        val archivedAccount = getArchivedAccount(accountId = accountId)

        val recoveredAccount = archivedAccount.recover()

        val updatedAccountList = add(accountList, recoveredAccount)

        saveAccounts(updatedAccountList)

        mainAccountTokensMigration.migrate(
            userWalletId = accountId.userWalletId,
            derivationIndex = recoveredAccount.derivationIndex,
        )

        refreshBalances(accountId = accountId)

        recoveredAccount
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

    private suspend fun Raise<Error>.getArchivedAccount(accountId: AccountId): ArchivedAccount {
        return catch(
            block = { crudRepository.getArchivedAccountSync(accountId = accountId) },
            catch = { raise(Error.DataOperationFailed(cause = it)) },
        )
            .getOrElse {
                raise(Error.DataOperationFailed(message = "Account not found: $accountId"))
            }
    }

    private fun ArchivedAccount.recover(): Account.CryptoPortfolio {
        return Account.CryptoPortfolio(
            accountId = this.accountId,
            accountName = this.name,
            icon = this.icon,
            derivationIndex = this.derivationIndex,
            cryptoCurrencies = emptySet(),
        )
    }

    private suspend fun Raise<Error>.saveAccounts(accountList: AccountList) {
        catch(
            block = { crudRepository.saveAccounts(accountList) },
            catch = { raise(Error.DataOperationFailed(cause = it)) },
        )
    }

    private fun Raise<Error>.add(accountList: AccountList, recoveredAccount: Account.CryptoPortfolio): AccountList {
        val maybeAccountList = accountList + recoveredAccount

        return maybeAccountList.mapLeft {
            val recoveredAccountName = recoveredAccount.accountName as? AccountName.Custom
                ?: raise(Error.DataOperationFailed(message = "Recovered account should have a custom name"))

            val indexedName = AccountNameIndexer.transform(from = recoveredAccountName.value)
                .let(AccountName::invoke).getOrNull()
                ?: raise(Error.DataOperationFailed(message = "Failed to generate indexed account name"))

            val renamedAccount = recoveredAccount.copy(accountName = indexedName)

            // recursively try to add the account with the new name
            add(accountList = accountList, recoveredAccount = renamedAccount)
        }
            .merge()
    }

    private suspend fun refreshBalances(accountId: AccountId): Either<Error, Unit> = either {
        val currencies = catch(
            block = { crudRepository.getAccountSync(accountId = accountId) },
            catch = { raise(Error.DataOperationFailed(cause = it)) },
        )
            .getOrElse { raise(Error.DataOperationFailed(message = "Account not found: $accountId")) }
            .cryptoCurrencies
            .toList()

        catch(
            block = {
                cryptoCurrencyBalanceFetcher(userWalletId = accountId.userWalletId, currencies = currencies)
            },
            catch = { raise(Error.DataOperationFailed(cause = it)) },
        )
    }

    /**
     * Represents possible errors that can occur during the add operation
     */
    sealed interface Error {

        val tag: String
            get() = this::class.simpleName ?: "RecoverCryptoPortfolioUseCase.Error"

        /**
         * Error indicating that the account list requirements were not met.
         *
         * @property cause the underlying cause of the error
         */
        data class AccountListRequirementsNotMet(val cause: AccountList.Error) : Error {
            override fun toString(): String = "$tag: Account list requirements not met: $cause"
        }

        /** Error indicating that a data operation failed */
        data class DataOperationFailed(val cause: Throwable) : Error {

            constructor(message: String) : this(cause = IllegalStateException(message))
        }
    }
}