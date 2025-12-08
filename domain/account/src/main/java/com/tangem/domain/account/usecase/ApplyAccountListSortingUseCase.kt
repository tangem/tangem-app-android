package com.tangem.domain.account.usecase

import arrow.core.Either
import arrow.core.getOrElse
import arrow.core.raise.*
import arrow.core.toNonEmptyListOrNull
import com.tangem.domain.account.models.AccountList
import com.tangem.domain.account.repository.AccountsCRUDRepository
import com.tangem.domain.core.utils.eitherOn
import com.tangem.domain.models.account.AccountId
import com.tangem.domain.models.wallet.UserWalletId
import com.tangem.utils.coroutines.CoroutineDispatcherProvider

/**
 * Use case to apply a specific sorting order to a list of accounts within a user's wallet.
 *
 * @property accountsCRUDRepository Repository for CRUD operations on accounts.
 * @property dispatchers Coroutine dispatcher provider for managing threading.
 *
[REDACTED_AUTHOR]
 */
class ApplyAccountListSortingUseCase(
    private val accountsCRUDRepository: AccountsCRUDRepository,
    private val dispatchers: CoroutineDispatcherProvider,
) {

    /**
     * Applies the sorting order of the provided list of accounts to the account list
     * associated with the user wallet ID of the first account in the list.
     *
     * @param accountIds List of AccountId representing the desired order.
     * @return Either an Error or Unit on successful completion.
     */
    suspend operator fun invoke(accountIds: List<AccountId>): Either<Error, Unit> =
        eitherOn(dispatcher = dispatchers.default) {
            val nonEmptyIds = accountIds.toNonEmptyListOrNull()
            ensureNotNull(nonEmptyIds) { Error.EmptyList }
            ensure(nonEmptyIds.size > 1) { Error.UnableToSortSingleAccount }

            val userWalletId = nonEmptyIds.first().userWalletId

            ensureNotNull(userWalletId) {
                Error.DataOperationFailed("UserWalletId is null in the accounts list")
            }

            val accountList = getAccountList(userWalletId)

            ensure(accountList.activeAccounts > 1) { Error.UnableToSortSingleAccount }

            val positionByAccountId = nonEmptyIds.withIndex().associate { it.value to it.index }
            val sortedAccounts = accountList.accounts.sortedBy {
                positionByAccountId[it.accountId] ?: raise(Error.SomeAccountsNotFound)
            }

            val updatedAccountList = withError(
                transform = { Error.DataOperationFailed("Unable to create AccountList: $it") },
            ) {
                AccountList.invoke(
                    userWalletId = accountList.userWalletId,
                    accounts = sortedAccounts,
                    totalAccounts = accountList.totalAccounts,
                    totalArchivedAccounts = accountList.totalArchivedAccounts,
                    sortType = accountList.sortType,
                    groupType = accountList.groupType,
                )
                    .bind()
            }

            if (updatedAccountList == accountList) {
                return@eitherOn
            }

            accountsCRUDRepository.saveAccounts(accountList = updatedAccountList)
        }

    private suspend fun Raise<Error>.getAccountList(userWalletId: UserWalletId): AccountList {
        return catch(
            block = { accountsCRUDRepository.getAccountListSync(userWalletId = userWalletId) },
            catch = { raise(Error.DataOperationFailed(cause = it)) },
        )
            .getOrElse {
                raise(Error.DataOperationFailed("Account list not found for wallet $userWalletId"))
            }
    }

    /**
     * Sealed interface representing possible errors that can occur during the application
     * of account list sorting.
     */
    sealed interface Error {

        data object EmptyList : Error

        data object UnableToSortSingleAccount : Error

        data object SomeAccountsNotFound : Error

        data class DataOperationFailed(val cause: Throwable) : Error {

            constructor(message: String) : this(cause = IllegalStateException(message))
        }
    }
}