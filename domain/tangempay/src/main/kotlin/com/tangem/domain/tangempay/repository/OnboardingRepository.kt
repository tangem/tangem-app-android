package com.tangem.domain.tangempay.repository

import arrow.core.Either
import com.tangem.core.error.UniversalError
import com.tangem.domain.models.account.BankCredentials
import com.tangem.domain.models.tangempay.TangemPayEligibilityType
import com.tangem.domain.models.tangempay.TangemPayCustomerResidency
import com.tangem.domain.models.tangempay.TangemPayProduct
import com.tangem.domain.models.wallet.UserWalletId
import com.tangem.domain.tangempay.model.AccountFee
import com.tangem.domain.tangempay.model.AccountFeeGroupType
import com.tangem.domain.tangempay.model.CustomerInfo
import com.tangem.domain.tangempay.model.CustomerOrderRequest
import com.tangem.domain.tangempay.error.TangemPayApiError
import com.tangem.domain.tangempay.model.TangemPayInitialCredentials
import kotlinx.coroutines.flow.Flow

@Suppress("TooManyFunctions")
interface OnboardingRepository {

    suspend fun validateDeeplink(link: String): Either<UniversalError, Boolean>

    suspend fun isTangemPayInitialDataProduced(userWalletId: UserWalletId): Boolean

    suspend fun produceInitialData(userWalletId: UserWalletId)

    suspend fun storeInitialCredentials(userWalletId: UserWalletId, initialCredentials: TangemPayInitialCredentials)

    suspend fun getCustomerInfo(userWalletId: UserWalletId): Either<TangemPayApiError, CustomerInfo>

    /** Fiat bank requisites for the wallet's Virtual Account on-ramp instance (VA MVP0, TWI-1638). */
    suspend fun getBankCredentials(
        userWalletId: UserWalletId,
        productInstanceId: String,
    ): Either<TangemPayApiError, BankCredentials>

    suspend fun getAllBankCredentials(
        userWalletId: UserWalletId,
        productInstanceId: String,
    ): Either<TangemPayApiError, List<BankCredentials>>

    suspend fun getFees(
        userWalletId: UserWalletId,
        product: TangemPayProduct,
        types: Set<AccountFeeGroupType>,
    ): Either<TangemPayApiError, List<AccountFee>>

    suspend fun createOrder(
        userWalletId: UserWalletId,
        request: CustomerOrderRequest.CardIssue,
        idempotencyKey: String,
    ): Either<TangemPayApiError, String>

    suspend fun clearOrderId(userWalletId: UserWalletId)

    suspend fun getOrderId(userWalletId: UserWalletId): String?

    /** Creates a Virtual Account on-ramp order (VA MVP0, TWI-1638); returns the created order id. */
    suspend fun createVirtualAccountOrder(
        userWalletId: UserWalletId,
        paymentAccountAddress: String,
        idempotencyKey: String,
    ): Either<TangemPayApiError, String>

    suspend fun getVirtualAccountOrderId(userWalletId: UserWalletId): String?

    suspend fun storeVirtualAccountOrderId(userWalletId: UserWalletId, vaOrderId: String)

    suspend fun clearVirtualAccountOrderId(userWalletId: UserWalletId)

    suspend fun hasVirtualAccountOrderFailed(userWalletId: UserWalletId): Boolean

    suspend fun storeVirtualAccountOrderFailed(userWalletId: UserWalletId, isFailed: Boolean)

    suspend fun hasTangemPayInWallet(userWalletId: UserWalletId): Either<TangemPayApiError, Boolean>

    suspend fun checkCustomerEligibility(): List<TangemPayEligibilityType>
    suspend fun getCustomerEligibility(): List<TangemPayEligibilityType>?

    /**
     * Fetches eligibility channels fresh via the user token (always hits the network, no cache read/write).
     * Differs from [checkCustomerEligibility] (static token, caches) and [getCustomerEligibility] (cache only).
     */
    suspend fun fetchCustomerEligibility(
        userWalletId: UserWalletId,
    ): Either<TangemPayApiError, List<TangemPayEligibilityType>>

    fun getSavedCustomerInfo(userWalletId: UserWalletId): CustomerInfo?

    fun isMainOnboardingBannerHidden(userWalletId: UserWalletId): Flow<Boolean>

    suspend fun setHideMainOnboardingBanner(userWalletId: UserWalletId)

    suspend fun disableTangemPay(userWalletId: UserWalletId): Either<TangemPayApiError, Unit>

    suspend fun isTangemPayDeactivated(userWalletId: UserWalletId): Boolean

    suspend fun storeCustomerResidency(userWalletId: UserWalletId, residency: TangemPayCustomerResidency)

    suspend fun getCustomerResidency(userWalletId: UserWalletId): TangemPayCustomerResidency?

    suspend fun clearCustomerResidency(userWalletId: UserWalletId)
}