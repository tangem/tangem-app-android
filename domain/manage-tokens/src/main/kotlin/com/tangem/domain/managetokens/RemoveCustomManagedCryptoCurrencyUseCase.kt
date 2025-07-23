package com.tangem.domain.managetokens

import arrow.core.Either
import com.tangem.domain.managetokens.model.ManagedCryptoCurrency
import com.tangem.domain.managetokens.repository.CustomTokensRepository
import com.tangem.domain.models.wallet.UserWalletId

class RemoveCustomManagedCryptoCurrencyUseCase(private val repository: CustomTokensRepository) {

    suspend operator fun invoke(
        userWalletId: UserWalletId,
        customCurrency: ManagedCryptoCurrency.Custom,
    ): Either<Throwable, Unit> {
        return Either.catch {
            repository.removeCurrency(userWalletId, customCurrency)
        }
    }
}