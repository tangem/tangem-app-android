package com.tangem.domain.accounts

import arrow.core.Either
import arrow.core.raise.*
import com.tangem.domain.accounts.error.UpdateAccountError
import com.tangem.domain.accounts.model.CryptoCurrenciesAccount
import com.tangem.domain.accounts.repository.CryptoCurrenciesAccountsRepository
import com.tangem.domain.accounts.utils.AccountValidator
import com.tangem.domain.wallets.models.UserWalletId

class UpdateAccountUseCase(
    private val repository: CryptoCurrenciesAccountsRepository,
) {

    private val validator = AccountValidator(repository)

    suspend fun launch(
        userWalletId: UserWalletId,
        accountId: CryptoCurrenciesAccount.ID,
        action: UpdateAction,
    ): Either<UpdateAccountError, Unit> = either {
        ensure(condition = accountId != CryptoCurrenciesAccount.ID.Main) {
            UpdateAccountError.CannotBeMainAccount
        }

        val isAccountUpdated = updateAccount(userWalletId, accountId, action)

        ensure(isAccountUpdated) { UpdateAccountError.NoAccountUpdated }
    }

    private suspend fun Raise<UpdateAccountError>.updateAccount(
        userWalletId: UserWalletId,
        accountId: CryptoCurrenciesAccount.ID,
        action: UpdateAction,
    ): Boolean = catch(
        block = {
            when (action) {
                is UpdateAction.Archive -> {
                    repository.archiveAccount(userWalletId, accountId)
                }
                is UpdateAction.Restore -> {
                    repository.restoreAccount(userWalletId, accountId)
                }
                is UpdateAction.ChangeTitle -> {
                    val title = withError({ UpdateAccountError.NoAccountUpdated }) {
                        with(validator) { validateTitle(action.newTitle) }
                    }

                    repository.changeAccountTitle(
                        userWalletId = userWalletId,
                        accountId = accountId,
                        newTitle = title,
                    )
                }
            }
        },
        catch = { raise(UpdateAccountError.DataError(it)) },
    )

    sealed class UpdateAction {

        data object Archive : UpdateAction()

        data object Restore : UpdateAction()

        data class ChangeTitle(val newTitle: String) : UpdateAction()
    }
}