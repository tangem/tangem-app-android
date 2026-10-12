package com.tangem.domain.pay.usecase

import arrow.core.Either
import com.tangem.domain.models.pay.TangemPayEligibility
import com.tangem.domain.models.pay.TangemPayProduct
import com.tangem.domain.pay.repository.TangemPayResidencyRepository
import com.tangem.domain.visa.error.VisaApiError

class CheckTangemPayEligibilityUseCase(
    private val residencyRepository: TangemPayResidencyRepository,
) {
    suspend operator fun invoke(
        product: TangemPayProduct,
        countryCode: String,
        regionCode: String?,
    ): Either<VisaApiError, TangemPayEligibility> = residencyRepository.checkEligibilityForCountry(
        product = product,
        countryCode = countryCode,
        regionCode = regionCode,
    )
}