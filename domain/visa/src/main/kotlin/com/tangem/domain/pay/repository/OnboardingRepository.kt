package com.tangem.domain.pay.repository

import arrow.core.Either
import com.tangem.core.error.UniversalError
import com.tangem.domain.models.wallet.UserWalletId
import com.tangem.domain.pay.model.CustomerInfo
import com.tangem.domain.visa.error.VisaApiError
import kotlinx.coroutines.Job

interface OnboardingRepository {

    suspend fun validateDeeplink(link: String): Either<UniversalError, Boolean>

    suspend fun isTangemPayInitialDataProduced(userWalletId: UserWalletId): Boolean

    suspend fun produceInitialData(userWalletId: UserWalletId)

    suspend fun getCustomerInfo(userWalletId: UserWalletId): Either<VisaApiError, CustomerInfo>

    suspend fun createOrder(userWalletId: UserWalletId): Job

    suspend fun clearOrderId(userWalletId: UserWalletId)

    suspend fun getOrderId(userWalletId: UserWalletId): String?

    suspend fun checkCustomerWallet(userWalletId: UserWalletId): Either<VisaApiError, Boolean>

    suspend fun checkCustomerEligibility(): Boolean

    fun getSavedCustomerInfo(userWalletId: UserWalletId): CustomerInfo?

    suspend fun getHideMainOnboardingBanner(userWalletId: UserWalletId): Boolean

    suspend fun setHideMainOnboardingBanner(userWalletId: UserWalletId)
}