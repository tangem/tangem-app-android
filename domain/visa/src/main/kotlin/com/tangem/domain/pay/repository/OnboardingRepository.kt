package com.tangem.domain.pay.repository

import arrow.core.Either
import com.tangem.core.error.UniversalError
import com.tangem.domain.pay.model.CustomerInfo

interface OnboardingRepository {

    suspend fun validateDeeplink(link: String): Either<UniversalError, Boolean>

    suspend fun getCustomerInfo(): Either<UniversalError, CustomerInfo>

    /**
     * Returns only if the user already authorised at least once
     */
    suspend fun getMainScreenCustomerInfo(): Either<UniversalError, CustomerInfo>
}