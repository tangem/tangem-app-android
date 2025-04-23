package com.tangem.domain.onramp

import arrow.core.Either
import com.tangem.domain.onramp.model.error.OnrampError
import com.tangem.domain.onramp.repositories.OnrampErrorResolver
import com.tangem.domain.onramp.repositories.OnrampRepository
import com.tangem.domain.wallets.models.UserWallet

class FetchOnrampCountriesUseCase(
    private val repository: OnrampRepository,
    private val errorResolver: OnrampErrorResolver,
) {

    suspend operator fun invoke(userWallet: UserWallet): Either<OnrampError, Unit> {
        return Either.catch<Unit> { repository.fetchCountries(userWallet) }.mapLeft(errorResolver::resolve)
    }
}