package com.tangem.domain.onramp

import arrow.core.Either
import com.tangem.domain.onramp.model.error.OnrampError
import com.tangem.domain.onramp.repositories.OnrampErrorResolver
import com.tangem.domain.onramp.repositories.OnrampRepository
import com.tangem.domain.models.wallet.UserWallet

class FetchOnrampCurrenciesUseCase(
    private val repository: OnrampRepository,
    private val errorResolver: OnrampErrorResolver,
) {

    suspend operator fun invoke(userWallet: UserWallet): Either<OnrampError, Unit> {
        return Either.catch { repository.fetchCurrencies(userWallet) }.mapLeft(errorResolver::resolve)
    }
}