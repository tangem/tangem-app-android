package com.tangem.domain.onramp

import arrow.core.Either
import com.tangem.domain.core.utils.EitherFlow
import com.tangem.domain.onramp.model.OnrampCountry
import com.tangem.domain.onramp.model.error.OnrampError
import com.tangem.domain.onramp.repositories.OnrampErrorResolver
import com.tangem.domain.onramp.repositories.OnrampRepository
import kotlinx.coroutines.flow.map

class GetOnrampCountriesUseCase(
    private val onrampRepository: OnrampRepository,
    private val errorResolver: OnrampErrorResolver,
) {

    operator fun invoke(): EitherFlow<OnrampError, List<OnrampCountry>> {
        return onrampRepository.getCountries().map {
            Either.catch { it }.mapLeft(errorResolver::resolve)
        }
    }
}