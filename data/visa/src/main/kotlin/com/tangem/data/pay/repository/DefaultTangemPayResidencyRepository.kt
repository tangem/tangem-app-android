package com.tangem.data.pay.repository

import arrow.core.Either
import com.tangem.data.pay.util.TangemPayCountryConverter
import com.tangem.data.pay.util.TangemPayEligibilityConverter
import com.tangem.data.pay.util.TangemPayRegionConverter
import com.tangem.domain.models.pay.TangemPayCountry
import com.tangem.domain.models.pay.TangemPayEligibility
import com.tangem.domain.models.pay.TangemPayProduct
import com.tangem.domain.models.pay.TangemPayRegion
import com.tangem.domain.models.pay.id
import com.tangem.domain.models.wallet.UserWalletId
import com.tangem.domain.pay.repository.TangemPayResidencyRepository
import com.tangem.domain.visa.error.VisaApiError
import com.tangem.spend.datasource.pay.TangemPayApi
import javax.inject.Inject

internal class DefaultTangemPayResidencyRepository @Inject constructor(
    private val tangemPayApi: TangemPayApi,
    private val requestHelper: TangemPayRequestPerformer,
) : TangemPayResidencyRepository {

    override suspend fun getCountries(): Either<VisaApiError, List<TangemPayCountry>> {
        return requestHelper.performWithStaticToken {
            tangemPayApi.getGeoCountries()
        }.map { response -> TangemPayCountryConverter.convertList(response.result) }
    }

    override suspend fun getRegions(countryCode: String): Either<VisaApiError, List<TangemPayRegion>> {
        return requestHelper.performWithStaticToken {
            tangemPayApi.getGeoRegions(countryCode = countryCode)
        }.map { response -> TangemPayRegionConverter.convertList(response.result) }
    }

    override suspend fun checkEligibilityForCountry(
        product: TangemPayProduct,
        countryCode: String,
        regionCode: String?,
    ): Either<VisaApiError, TangemPayEligibility> {
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
    ): Either<VisaApiError, TangemPayEligibility> {
        return requestHelper.performRequest(userWalletId) { authHeader ->
            tangemPayApi.checkEligibilityForCustomer(authHeader = authHeader, product = product.id)
        }.map { response -> TangemPayEligibilityConverter.convert(response.result) }
    }
}