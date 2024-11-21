package com.tangem.domain.onramp

import arrow.core.Either
import com.tangem.domain.onramp.model.OnrampCountry
import com.tangem.domain.onramp.repositories.OnrampRepository

class GetOnrampCountriesUseCase(private val onrampRepository: OnrampRepository) {

    suspend operator fun invoke(): Either<Throwable, List<OnrampCountry>> {
        return Either.catch { onrampRepository.getCountries() }
    }
}