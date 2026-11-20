package com.tangem.domain.tangempay.usecase

import arrow.core.Either
import com.tangem.domain.models.tangempay.TangemPayCountry
import com.tangem.domain.tangempay.repository.TangemPayResidencyRepository
import com.tangem.domain.tangempay.error.TangemPayApiError

class GetTangemPayCountriesUseCase(
    private val residencyRepository: TangemPayResidencyRepository,
) {
    suspend operator fun invoke(): Either<TangemPayApiError, List<TangemPayCountry>> {
        return residencyRepository.getCountries()
    }
}