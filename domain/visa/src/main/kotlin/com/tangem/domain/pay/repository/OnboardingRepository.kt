package com.tangem.domain.pay.repository

import arrow.core.Either
import com.tangem.core.error.UniversalError
import com.tangem.domain.models.account.BankCredentials
import com.tangem.domain.models.pay.TangemPayEligibilityType
import com.tangem.domain.models.wallet.UserWalletId
import com.tangem.domain.pay.model.CustomerInfo
import com.tangem.domain.visa.error.VisaApiError

interface OnboardingRepository {

    suspend fun validateDeeplink(link: String): Either<UniversalError, Boolean>

    suspend fun isTangemPayInitialDataProduced(userWalletId: UserWalletId): Boolean

    suspend fun produceInitialData(userWalletId: UserWalletId)

    suspend fun getCustomerInfo(userWalletId: UserWalletId): Either<VisaApiError, CustomerInfo>

    /** Fiat bank requisites for the wallet's Virtual Account on-ramp instance (VA MVP0, TWI-1638). */
    suspend fun getBankCredentials(
        userWalletId: UserWalletId,
        productInstanceId: String,
    ): Either<VisaApiError, BankCredentials>

    suspend fun createOrder(userWalletId: UserWalletId): Either<VisaApiError, String>

    suspend fun clearOrderId(userWalletId: UserWalletId)

    suspend fun getOrderId(userWalletId: UserWalletId): String?

    suspend fun hasTangemPayInWallet(userWalletId: UserWalletId): Either<VisaApiError, Boolean>

    suspend fun checkCustomerEligibility(): List<TangemPayEligibilityType>
    suspend fun getCustomerEligibility(): List<TangemPayEligibilityType>

    /**
     * Fetches eligibility channels fresh via the user token (always hits the network, no cache read/write).
     * Differs from [checkCustomerEligibility] (static token, caches) and [getCustomerEligibility] (cache only).
     */
    suspend fun fetchCustomerEligibility(
        userWalletId: UserWalletId,
    ): Either<VisaApiError, List<TangemPayEligibilityType>>

    fun getSavedCustomerInfo(userWalletId: UserWalletId): CustomerInfo?

    suspend fun getHideMainOnboardingBanner(userWalletId: UserWalletId): Boolean

    suspend fun setHideMainOnboardingBanner(userWalletId: UserWalletId)

    suspend fun disableTangemPay(userWalletId: UserWalletId): Either<VisaApiError, Unit>

    suspend fun isTangemPayDeactivated(userWalletId: UserWalletId): Boolean
}