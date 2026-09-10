package com.tangem.domain.pay.usecase

import arrow.core.Either
import com.tangem.domain.models.pay.TangemPayRegion
import com.tangem.domain.pay.repository.TangemPayResidencyRepository
import com.tangem.domain.visa.error.VisaApiError

class GetTangemPayRegionsUseCase(
    private val residencyRepository: TangemPayResidencyRepository,
) {
    suspend operator fun invoke(countryCode: String): Either<VisaApiError, List<TangemPayRegion>> {
        return residencyRepository.getRegions(countryCode)
    }
}