package com.tangem.data.pay.repository

import arrow.core.Either
import arrow.core.left
import arrow.core.right
import com.tangem.domain.models.pay.TangemPayCountry
import com.tangem.domain.models.pay.TangemPayEligibility
import com.tangem.domain.models.pay.TangemPayProduct
import com.tangem.domain.models.pay.TangemPayRegion
import com.tangem.domain.models.wallet.UserWalletId
import com.tangem.domain.pay.repository.TangemPayResidencyRepository
import com.tangem.domain.visa.error.VisaApiError
import kotlinx.coroutines.delay

/**
 * In-memory stand-in for the geo endpoints while their country list is still being filled in.
 * Only [getCountries] and [getRegions] answer from [MockTangemPayResidencySeed]; both eligibility checks stay on the
 * real backend through [origin], so the onboarding gate keeps behaving as it does in production.
 */
internal class MockTangemPayResidencyRepository(
    private val origin: TangemPayResidencyRepository,
) : TangemPayResidencyRepository {

    override suspend fun getCountries(): Either<VisaApiError, List<TangemPayCountry>> {
        delay(FETCH_DELAY_MILLIS)
        return MockTangemPayResidencySeed.countries.right()
    }

    override suspend fun getRegions(countryCode: String): Either<VisaApiError, List<TangemPayRegion>> {
        delay(FETCH_DELAY_MILLIS)
        val regions = MockTangemPayResidencySeed.regions[countryCode.uppercase()]
        return regions?.right() ?: VisaApiError.NotFound.left()
    }

    override suspend fun checkEligibilityForCountry(
        product: TangemPayProduct,
        countryCode: String,
        regionCode: String?,
    ): Either<VisaApiError, TangemPayEligibility> =
        origin.checkEligibilityForCountry(product = product, countryCode = countryCode, regionCode = regionCode)

    override suspend fun checkEligibilityForCustomer(
        userWalletId: UserWalletId,
        product: TangemPayProduct,
    ): Either<VisaApiError, TangemPayEligibility> =
        origin.checkEligibilityForCustomer(userWalletId = userWalletId, product = product)
}

private const val FETCH_DELAY_MILLIS = 500L