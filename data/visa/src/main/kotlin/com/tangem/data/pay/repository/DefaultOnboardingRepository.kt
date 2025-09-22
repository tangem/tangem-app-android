package com.tangem.data.pay.repository

import arrow.core.Either
import arrow.core.raise.either
import com.tangem.core.error.UniversalError
import com.tangem.datasource.api.common.response.getOrThrow
import com.tangem.datasource.api.pay.TangemPayApi
import com.tangem.datasource.api.pay.models.request.DeeplinkValidityRequest
import com.tangem.domain.pay.model.CustomerInfo
import com.tangem.domain.pay.model.ProductInstance
import com.tangem.domain.pay.repository.OnboardingRepository
import com.tangem.domain.visa.error.VisaApiError
import javax.inject.Inject

private const val VALID_STATUS = "valid"

internal class DefaultOnboardingRepository @Inject constructor(
    private val tangemPayApi: TangemPayApi,
    private val requestHelper: TangemPayRequestPerformer,
) : OnboardingRepository {

    override suspend fun validateDeeplink(link: String): Either<UniversalError, Boolean> = either {
        return requestHelper.request {
            tangemPayApi.validateDeeplink(DeeplinkValidityRequest(link)).getOrThrow().result
                ?: raise(VisaApiError.UnknownWithoutCode)
        }.map { result -> result.status == VALID_STATUS }
    }

    override suspend fun getCustomerInfo(): Either<UniversalError, CustomerInfo> = either {
        return requestHelper.request { authHeader ->
            val response = tangemPayApi.getCustomerMe(authHeader).getOrThrow()
            response.result ?: raise(VisaApiError.UnknownWithoutCode)
        }.map { result ->
            CustomerInfo(
                productInstance = result.productInstance?.let { ProductInstance(id = it.id, status = it.status) },
                kycStatus = result.kyc?.status,
            )
        }
    }
}