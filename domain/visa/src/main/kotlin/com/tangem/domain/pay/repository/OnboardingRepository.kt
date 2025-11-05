package com.tangem.domain.pay.repository

import arrow.core.Either
import com.tangem.core.error.UniversalError
import com.tangem.domain.pay.model.CustomerInfo
import com.tangem.domain.pay.model.MainScreenCustomerInfo

interface OnboardingRepository {

    suspend fun validateDeeplink(link: String): Either<UniversalError, Boolean>

    suspend fun isTangemPayInitialDataProduced(): Boolean

    suspend fun produceInitialData()

    suspend fun getCustomerInfo(): Either<UniversalError, CustomerInfo>

    suspend fun createOrder(): Either<UniversalError, Unit>

    /**
     * Returns only if the user already authorised at least once
     */
    suspend fun getMainScreenCustomerInfo(): Either<UniversalError, MainScreenCustomerInfo>
}