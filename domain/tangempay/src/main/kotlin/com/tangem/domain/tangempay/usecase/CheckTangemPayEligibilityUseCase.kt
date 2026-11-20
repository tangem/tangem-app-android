package com.tangem.domain.tangempay.usecase

import arrow.core.Either
import com.tangem.domain.models.tangempay.TangemPayEligibility
import com.tangem.domain.models.tangempay.TangemPayProduct
import com.tangem.domain.tangempay.repository.TangemPayResidencyRepository
import com.tangem.domain.tangempay.error.TangemPayApiError

class CheckTangemPayEligibilityUseCase(
    private val residencyRepository: TangemPayResidencyRepository,
) {
    suspend operator fun invoke(
        product: TangemPayProduct,
        countryCode: String,
        regionCode: String?,
    ): Either<TangemPayApiError, TangemPayEligibility> = residencyRepository.checkEligibilityForCountry(
        product = product,
        countryCode = countryCode,
        regionCode = regionCode,
    )
}