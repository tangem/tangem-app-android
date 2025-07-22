package com.tangem.domain.managetokens

import arrow.core.Either
import com.tangem.domain.managetokens.model.CurrencyUnsupportedState
import com.tangem.domain.managetokens.model.ManagedCryptoCurrency
import com.tangem.domain.managetokens.repository.ManageTokensRepository
import com.tangem.domain.models.wallet.UserWalletId

class CheckCurrencyUnsupportedUseCase(
    private val repository: ManageTokensRepository,
) {

    suspend operator fun invoke(
        userWalletId: UserWalletId,
        sourceNetwork: ManagedCryptoCurrency.SourceNetwork,
    ): Either<Throwable, CurrencyUnsupportedState?> {
        return Either.catch {
            repository.checkCurrencyUnsupportedState(userWalletId, sourceNetwork)
        }
    }

    suspend operator fun invoke(
        userWalletId: UserWalletId,
        networkId: String,
        isMainNetwork: Boolean,
    ): Either<Throwable, CurrencyUnsupportedState?> {
        return Either.catch {
            repository.checkCurrencyUnsupportedState(
                userWalletId = userWalletId,
                rawNetworkId = networkId,
                isMainNetwork = isMainNetwork,
            )
        }
    }
}