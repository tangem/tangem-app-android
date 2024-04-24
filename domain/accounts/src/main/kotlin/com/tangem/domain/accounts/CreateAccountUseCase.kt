package com.tangem.domain.accounts

import arrow.core.Either
import arrow.core.raise.catch
import arrow.core.raise.either
import arrow.core.raise.ensure
import arrow.core.raise.withError
import com.tangem.domain.accounts.error.CheckAccountError
import com.tangem.domain.accounts.error.CreateAccountError
import com.tangem.domain.accounts.repository.CryptoCurrenciesAccountsRepository
import com.tangem.domain.accounts.utils.AccountValidator
import com.tangem.domain.wallets.models.UserWalletId

class CreateAccountUseCase(
    private val repository: CryptoCurrenciesAccountsRepository,
) {

    private val validator = AccountValidator(repository)

    suspend fun launch(userWalletId: UserWalletId, id: Int, title: String): Either<CreateAccountError, Unit> = either {
        val account = withError({ CreateAccountError.NoAccountCreated(it) }) {
            validator.validateAndGet(userWalletId, id, title).bind()
        }

        val isAccountNotExist = catch({ repository.createAccountIfNot(userWalletId, account) }) {
            raise(CreateAccountError.DataError(it))
        }

        ensure(isAccountNotExist) {
            CreateAccountError.NoAccountCreated(cause = CheckAccountError.AlreadyCreated)
        }
    }
}