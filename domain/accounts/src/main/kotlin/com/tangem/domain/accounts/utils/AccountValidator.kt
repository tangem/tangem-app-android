package com.tangem.domain.accounts.utils

import arrow.core.Either
import arrow.core.raise.Raise
import arrow.core.raise.catch
import arrow.core.raise.either
import arrow.core.raise.ensure
import com.tangem.domain.accounts.error.CheckAccountError
import com.tangem.domain.accounts.model.CryptoCurrenciesAccount
import com.tangem.domain.accounts.repository.CryptoCurrenciesAccountsRepository
import com.tangem.domain.wallets.models.UserWalletId

internal class AccountValidator(
    private val repository: CryptoCurrenciesAccountsRepository,
) {

    suspend fun validate(
        userWalletId: UserWalletId,
        idValue: Int,
        titleValue: String,
    ): Either<CheckAccountError, Unit> = either {
        validateIdAndTitle(idValue, titleValue)

        checkIsAlreadyCreated(userWalletId, idValue)
    }

    suspend fun validateAndGet(
        userWalletId: UserWalletId,
        idValue: Int,
        titleValue: String,
    ): Either<CheckAccountError, CryptoCurrenciesAccount> = either {
        val (id, title) = validateIdAndTitle(idValue, titleValue)
        checkIsAlreadyCreated(userWalletId, idValue)

        CryptoCurrenciesAccount(
            id,
            title,
            currenciesCount = 0,
            isArchived = false,
        )
    }

    private fun Raise<CheckAccountError>.validateIdAndTitle(
        idValue: Int,
        titleValue: String,
    ): Pair<CryptoCurrenciesAccount.ID, CryptoCurrenciesAccount.Title> {
        val id = validateId(idValue)
        val title = validateTitle(titleValue)

        return id to title
    }

    fun Raise<CheckAccountError>.validateTitle(titleValue: String) =
        catch({ CryptoCurrenciesAccount.Title.Default(titleValue) }) {
            raise(CheckAccountError.WrongTitle(it))
        }

    private fun Raise<CheckAccountError>.validateId(id: Int): CryptoCurrenciesAccount.ID {
        ensure(condition = id != CryptoCurrenciesAccount.ID.MAIN_ACCOUNT_ID) {
            CheckAccountError.CannotBeMainAccount
        }
        return catch({ CryptoCurrenciesAccount.ID.Default(id) }) {
            raise(CheckAccountError.WrongId(it))
        }
    }

    private suspend fun Raise<CheckAccountError>.checkIsAlreadyCreated(userWalletId: UserWalletId, id: Int) {
        val accounts = getAccounts(userWalletId)
        ensure(accounts.none { it.id.value == id }) { CheckAccountError.AlreadyCreated }
    }

    private suspend fun Raise<CheckAccountError>.getAccounts(
        userWalletId: UserWalletId,
    ): List<CryptoCurrenciesAccount> = catch(
        block = { repository.getAccounts(userWalletId, refresh = false) },
        catch = { raise(CheckAccountError.DataError(it)) },
    )
}