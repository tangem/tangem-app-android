package com.tangem.domain.pay.repository

import arrow.core.Either
import com.tangem.domain.models.pay.TangemPayCountry
import com.tangem.domain.models.pay.TangemPayEligibility
import com.tangem.domain.models.pay.TangemPayProduct
import com.tangem.domain.models.pay.TangemPayRegion
import com.tangem.domain.models.wallet.UserWalletId
import com.tangem.domain.visa.error.VisaApiError

interface TangemPayResidencyRepository {

    suspend fun getCountries(): Either<VisaApiError, List<TangemPayCountry>>

    suspend fun getRegions(countryCode: String): Either<VisaApiError, List<TangemPayRegion>>

    suspend fun checkEligibilityForCountry(
        product: TangemPayProduct,
        countryCode: String,
        regionCode: String?,
    ): Either<VisaApiError, TangemPayEligibility>

    suspend fun checkEligibilityForCustomer(
        userWalletId: UserWalletId,
        product: TangemPayProduct,
    ): Either<VisaApiError, TangemPayEligibility>
}