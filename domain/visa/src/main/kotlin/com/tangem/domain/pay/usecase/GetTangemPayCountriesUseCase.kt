package com.tangem.domain.pay.usecase

import arrow.core.Either
import com.tangem.domain.models.pay.TangemPayCountry
import com.tangem.domain.pay.repository.TangemPayResidencyRepository
import com.tangem.domain.visa.error.VisaApiError

class GetTangemPayCountriesUseCase(
    private val residencyRepository: TangemPayResidencyRepository,
) {
    suspend operator fun invoke(): Either<VisaApiError, List<TangemPayCountry>> {
        return residencyRepository.getCountries()
    }
}