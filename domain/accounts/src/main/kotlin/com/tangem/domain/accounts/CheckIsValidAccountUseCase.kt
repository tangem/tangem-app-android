package com.tangem.domain.accounts

import arrow.core.Either
import com.tangem.domain.accounts.error.CheckAccountError
import com.tangem.domain.accounts.repository.CryptoCurrenciesAccountsRepository
import com.tangem.domain.accounts.utils.AccountValidator
import com.tangem.domain.wallets.models.UserWalletId

class CheckIsValidAccountUseCase(repository: CryptoCurrenciesAccountsRepository) {

    private val validator = AccountValidator(repository)

    suspend fun launch(userWalletId: UserWalletId, id: Int, title: String): Either<CheckAccountError, Unit> =
        validator.validate(userWalletId, id, title)
}