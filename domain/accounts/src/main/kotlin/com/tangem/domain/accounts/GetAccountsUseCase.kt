package com.tangem.domain.accounts

import arrow.core.Either
import arrow.core.raise.catch
import arrow.core.raise.either
import com.tangem.domain.accounts.error.AccountsError
import com.tangem.domain.accounts.model.CryptoCurrenciesAccount
import com.tangem.domain.accounts.repository.CryptoCurrenciesAccountsRepository
import com.tangem.domain.core.lce.LceFlow
import com.tangem.domain.core.utils.lceContent
import com.tangem.domain.core.utils.lceError
import com.tangem.domain.core.utils.lceLoading
import com.tangem.domain.wallets.models.UserWalletId
import kotlinx.coroutines.flow.map

class GetAccountsUseCase(
    private val repository: CryptoCurrenciesAccountsRepository,
) {

    suspend fun launch(
        userWalletId: UserWalletId,
        refresh: Boolean = false,
    ): Either<AccountsError, List<CryptoCurrenciesAccount>> = either {
        val accounts = catch({ repository.getAccounts(userWalletId, refresh) }) {
            raise(AccountsError.DataError(it))
        }

        accounts.ifEmpty { raise(AccountsError.NoAccounts) }
    }

    fun launchFlow(userWalletId: UserWalletId): LceFlow<AccountsError, List<CryptoCurrenciesAccount>> {
        return repository.getAccountsUpdates(userWalletId)
            .map { maybeAccounts ->
                maybeAccounts.fold(
                    ifLoading = { lceLoading(it) },
                    ifContent = { accounts ->
                        if (accounts.isEmpty()) {
                            AccountsError.NoAccounts.lceError()
                        } else {
                            accounts.lceContent()
                        }
                    },
                    ifError = { AccountsError.DataError(it).lceError() },
                )
            }
    }
}