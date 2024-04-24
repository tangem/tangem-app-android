package com.tangem.domain.accounts

import arrow.core.Either
import arrow.core.raise.catch
import arrow.core.raise.either
import arrow.core.raise.ensure
import com.tangem.domain.accounts.error.SelectedAccountError
import com.tangem.domain.accounts.model.CryptoCurrenciesAccount
import com.tangem.domain.accounts.repository.CryptoCurrenciesAccountsRepository
import com.tangem.domain.wallets.models.UserWalletId

class SelectAccountUseCase(
    private val repository: CryptoCurrenciesAccountsRepository,
) {

    suspend fun launch(
        userWalletId: UserWalletId,
        accountId: CryptoCurrenciesAccount.ID,
    ): Either<SelectedAccountError, Unit> = either {
        val isAccountSelected = catch({ repository.selectAccountIfPresent(userWalletId, accountId) }) {
            raise(SelectedAccountError.DataError(it))
        }

        ensure(isAccountSelected) { SelectedAccountError.NoAccountSelected }
    }
}