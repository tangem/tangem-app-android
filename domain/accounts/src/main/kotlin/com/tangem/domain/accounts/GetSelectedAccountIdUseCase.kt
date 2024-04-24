package com.tangem.domain.accounts

import arrow.core.Either
import arrow.core.left
import arrow.core.right
import com.tangem.domain.accounts.error.SelectedAccountError
import com.tangem.domain.accounts.model.CryptoCurrenciesAccount
import com.tangem.domain.accounts.repository.CryptoCurrenciesAccountsRepository
import com.tangem.domain.core.utils.EitherFlow
import com.tangem.domain.wallets.models.UserWalletId
import kotlinx.coroutines.flow.catch
import kotlinx.coroutines.flow.firstOrNull
import kotlinx.coroutines.flow.map

class GetSelectedAccountIdUseCase(
    private val repository: CryptoCurrenciesAccountsRepository,
) {

    suspend fun launch(userWalletId: UserWalletId): Either<SelectedAccountError, CryptoCurrenciesAccount.ID> {
        return getSelectedAccountIdUpdates(userWalletId)
            .firstOrNull()
            ?: SelectedAccountError.NoAccountSelected.left()
    }

    fun launchFlow(userWalletId: UserWalletId): EitherFlow<SelectedAccountError, CryptoCurrenciesAccount.ID> {
        return getSelectedAccountIdUpdates(userWalletId)
    }

    private fun getSelectedAccountIdUpdates(
        userWalletId: UserWalletId,
    ): EitherFlow<SelectedAccountError, CryptoCurrenciesAccount.ID> {
        return repository.getSelectedAccountIdUpdates(userWalletId)
            .map<CryptoCurrenciesAccount.ID, Either<SelectedAccountError, CryptoCurrenciesAccount.ID>> { it.right() }
            .catch {
                val e = SelectedAccountError.DataError(it)
                emit(e.left())
            }
    }
}