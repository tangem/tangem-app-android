package com.tangem.data.tangempay.repository

import arrow.core.Either
import com.tangem.data.tangempay.util.TangemPayCountryConverter
import com.tangem.data.tangempay.util.TangemPayEligibilityConverter
import com.tangem.data.tangempay.util.TangemPayRegionConverter
import com.tangem.domain.models.tangempay.TangemPayCountry
import com.tangem.domain.models.tangempay.TangemPayEligibility
import com.tangem.domain.models.tangempay.TangemPayProduct
import com.tangem.domain.models.tangempay.TangemPayRegion
import com.tangem.domain.models.tangempay.id
import com.tangem.domain.models.wallet.UserWalletId
import com.tangem.domain.tangempay.repository.TangemPayResidencyRepository
import com.tangem.domain.tangempay.error.TangemPayApiError
import com.tangem.spend.datasource.tangempay.TangemPayApi
import javax.inject.Inject

internal class DefaultTangemPayResidencyRepository @Inject constructor(
    private val tangemPayApi: TangemPayApi,
    private val requestHelper: TangemPayRequestPerformer,
) : TangemPayResidencyRepository {

    override suspend fun getCountries(): Either<TangemPayApiError, List<TangemPayCountry>> {
        return requestHelper.performWithStaticToken {
            tangemPayApi.getGeoCountries()
        }.map { response -> TangemPayCountryConverter.convertList(response.result) }
    }

    override suspend fun getRegions(countryCode: String): Either<TangemPayApiError, List<TangemPayRegion>> {
        return requestHelper.performWithStaticToken {
            tangemPayApi.getGeoRegions(countryCode = countryCode)
        }.map { response -> TangemPayRegionConverter.convertList(response.result) }
    }

    override suspend fun checkEligibilityForCountry(
        product: TangemPayProduct,
        countryCode: String,
        regionCode: String?,
    ): Either<TangemPayApiError, TangemPayEligibility> {
        return requestHelper.performWithStaticToken {
            tangemPayApi.checkEligibilityForCountry(
                product = product.id,
                country = countryCode,
                region = regionCode,
            )
        }.map { response -> TangemPayEligibilityConverter.convert(response.result) }
    }

    override suspend fun checkEligibilityForCustomer(
        userWalletId: UserWalletId,
        product: TangemPayProduct,
    ): Either<TangemPayApiError, TangemPayEligibility> {
        return requestHelper.performRequest(userWalletId) { authHeader ->
            tangemPayApi.checkEligibilityForCustomer(authHeader = authHeader, product = product.id)
        }.map { response -> TangemPayEligibilityConverter.convert(response.result) }
    }
}