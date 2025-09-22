package com.tangem.data.pay.repository

import arrow.core.Either
import arrow.core.raise.either
import com.tangem.core.error.UniversalError
import com.tangem.datasource.api.pay.TangemPayApi
import com.tangem.datasource.api.pay.models.request.DeeplinkValidityRequest
import com.tangem.domain.pay.model.CustomerInfo
import com.tangem.domain.pay.model.ProductInstance
import com.tangem.domain.pay.repository.OnboardingRepository
import javax.inject.Inject

private const val VALID_STATUS = "valid"

internal class DefaultOnboardingRepository @Inject constructor(
    private val tangemPayApi: TangemPayApi,
    private val requestHelper: TangemPayRequestPerformer,
) : OnboardingRepository {

    override suspend fun validateDeeplink(link: String): Either<UniversalError, Boolean> = either {
        return requestHelper.request {
            tangemPayApi.validateDeeplink(DeeplinkValidityRequest(link))
        }.map { it.result?.status == VALID_STATUS }
    }

    override suspend fun getCustomerInfo(): Either<UniversalError, CustomerInfo> = either {
        return requestHelper.request { authHeader ->
            tangemPayApi.getCustomerMe(authHeader)
        }.map {
            CustomerInfo(
                productInstance = it.result?.productInstance?.let { ProductInstance(id = it.id, status = it.status) },
                kycStatus = it.result?.kyc?.status,
            )
        }
    }
}