package com.tangem.domain.tangempay.usecase

import arrow.core.Either
import com.tangem.domain.models.tangempay.TangemPayRegion
import com.tangem.domain.tangempay.repository.TangemPayResidencyRepository
import com.tangem.domain.tangempay.error.TangemPayApiError

class GetTangemPayRegionsUseCase(
    private val residencyRepository: TangemPayResidencyRepository,
) {
    suspend operator fun invoke(countryCode: String): Either<TangemPayApiError, List<TangemPayRegion>> {
        return residencyRepository.getRegions(countryCode)
    }
}