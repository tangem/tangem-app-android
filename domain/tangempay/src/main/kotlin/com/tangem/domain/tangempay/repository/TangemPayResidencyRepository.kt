package com.tangem.domain.tangempay.repository

import arrow.core.Either
import com.tangem.domain.models.tangempay.TangemPayCountry
import com.tangem.domain.models.tangempay.TangemPayEligibility
import com.tangem.domain.models.tangempay.TangemPayProduct
import com.tangem.domain.models.tangempay.TangemPayRegion
import com.tangem.domain.models.wallet.UserWalletId
import com.tangem.domain.tangempay.error.TangemPayApiError

interface TangemPayResidencyRepository {

    suspend fun getCountries(): Either<TangemPayApiError, List<TangemPayCountry>>

    suspend fun getRegions(countryCode: String): Either<TangemPayApiError, List<TangemPayRegion>>

    suspend fun checkEligibilityForCountry(
        product: TangemPayProduct,
        countryCode: String,
        regionCode: String?,
    ): Either<TangemPayApiError, TangemPayEligibility>

    suspend fun checkEligibilityForCustomer(
        userWalletId: UserWalletId,
        product: TangemPayProduct,
    ): Either<TangemPayApiError, TangemPayEligibility>
}