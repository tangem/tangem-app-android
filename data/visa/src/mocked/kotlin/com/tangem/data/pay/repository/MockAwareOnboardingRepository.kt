package com.tangem.data.pay.repository

import com.tangem.spend.datasource.config.TangemPay

import arrow.core.Either
import arrow.core.right
import com.tangem.core.error.UniversalError
import com.tangem.core.remote.config.ApiEnvironment
import com.tangem.core.remote.config.managers.ApiConfigsManager
import com.tangem.data.pay.store.TangemPayStorage
import com.tangem.domain.models.account.BankCredentials
import com.tangem.domain.models.account.TangemPayOnrampFee
import com.tangem.domain.models.pay.TangemPayEligibilityType
import com.tangem.domain.models.wallet.UserWalletId
import com.tangem.domain.pay.model.CustomerInfo
import com.tangem.domain.pay.model.OrderType
import com.tangem.domain.pay.repository.OnboardingRepository
import com.tangem.domain.visa.error.VisaApiError
import java.util.concurrent.ConcurrentHashMap
import javax.inject.Inject
import javax.inject.Singleton
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.flowOf

/**
 * In MOCK env only skips the NFC-signing enrollment steps (order ids, initial credentials). The
 * customer-facing state — whether a wallet has Tangem Pay ([hasTangemPayInWallet]), KYC status, ACTIVE /
 * INACTIVE, balances ([getCustomerInfo]) — is driven by the WireMock test scenario (authenticated with the
 * synthetic tokens from [com.tangem.data.pay.store.MockAwareTangemPayStorage]) rather than hardcoded:
 *  - [hasTangemPayInWallet] delegates to the real repo, so the "existing customer" gate follows the
 *    checkCustomerWalletId mock (the `tangem_pay_eligibility` scenario: `Started` → 404/NotFound →
 *    no Payment account, `PaeraCustomer` → 200 → Payment account);
 *  - [getCustomerInfo] delegates to the real repo (WireMock), so KYC / customer-state scenarios take effect;
 *  - [validateDeeplink] delegates to the real repo, so the `tangem_pay_deeplink_validate` scenario decides
 *    VALID / INVALID.
 */
@Singleton
internal class MockAwareOnboardingRepository @Inject constructor(
    private val real: DefaultOnboardingRepository,
    private val apiConfigsManager: ApiConfigsManager,
    private val tangemPayStorage: TangemPayStorage,
) : OnboardingRepository {

    private val mockOrderIds: MutableSet<UserWalletId> = ConcurrentHashMap.newKeySet()
    private val mockVaOrderIds: MutableSet<UserWalletId> = ConcurrentHashMap.newKeySet()

    private val isMockMode: Boolean
        get() = apiConfigsManager
            .getEnvironmentConfig(TangemPay.Bff.ID)
            .environment == ApiEnvironment.MOCK

    override suspend fun validateDeeplink(link: String): Either<UniversalError, Boolean> = real.validateDeeplink(link)

    override suspend fun isTangemPayInitialDataProduced(userWalletId: UserWalletId): Boolean {
        if (isMockMode) return true
        return real.isTangemPayInitialDataProduced(userWalletId)
    }

    override suspend fun produceInitialData(userWalletId: UserWalletId) {
        if (isMockMode) {
            // Credentials come from MockAwareTangemPayStorage, but the "this wallet is a Paera customer" flag
            // is the real thing the enrollment writes — without it the wallet keeps the cached 404 answer and
            // no Payment account ever appears after accepting the offer.
            tangemPayStorage.storeCheckCustomerWalletResult(userWalletId = userWalletId, isPaeraCustomer = true)
            return
        }
        real.produceInitialData(userWalletId)
    }

    // Delegates to WireMock (via the real repo + synthetic storage tokens) so the customer state — KYC status,
    // ACTIVE/INACTIVE, balances — follows the test scenario instead of a hardcoded "always active" customer.
    // Note: this may be invoked even when `hasTangemPayInWallet` is false (e.g., onboarding/deeplink flows),
    // so tests must provide the corresponding WireMock mappings.
    override suspend fun getCustomerInfo(userWalletId: UserWalletId): Either<VisaApiError, CustomerInfo> =
        real.getCustomerInfo(userWalletId)

    override suspend fun getBankCredentials(
        userWalletId: UserWalletId,
        productInstanceId: String,
    ): Either<VisaApiError, BankCredentials> = real.getBankCredentials(userWalletId, productInstanceId)

    override suspend fun getOnrampFees(
        userWalletId: UserWalletId,
    ): Either<VisaApiError, List<TangemPayOnrampFee>> = real.getOnrampFees(userWalletId)

    override suspend fun createOrder(
        userWalletId: UserWalletId,
        deeplinkValue: String?,
        specificationName: String,
        type: OrderType,
    ): Either<VisaApiError, String> {
        if (isMockMode) {
            mockOrderIds.add(userWalletId)
            return MOCK_ORDER_ID.right()
        }
        return real.createOrder(
            userWalletId = userWalletId,
            deeplinkValue = deeplinkValue,
            specificationName = specificationName,
            type = type,
        )
    }

    override suspend fun clearOrderId(userWalletId: UserWalletId) {
        if (isMockMode) {
            mockOrderIds.remove(userWalletId)
            return
        }
        real.clearOrderId(userWalletId)
    }

    override suspend fun getOrderId(userWalletId: UserWalletId): String? {
        if (isMockMode) return MOCK_ORDER_ID.takeIf { userWalletId in mockOrderIds }
        return real.getOrderId(userWalletId)
    }

    override suspend fun createVirtualAccountOrder(
        userWalletId: UserWalletId,
        paymentAccountAddress: String,
        idempotencyKey: String,
    ): Either<VisaApiError, String> {
        if (isMockMode) {
            mockVaOrderIds.add(userWalletId)
            return MOCK_VA_ORDER_ID.right()
        }
        return real.createVirtualAccountOrder(userWalletId, paymentAccountAddress, idempotencyKey)
    }

    override suspend fun getVirtualAccountOrderId(userWalletId: UserWalletId): String? {
        if (isMockMode) return MOCK_VA_ORDER_ID.takeIf { userWalletId in mockVaOrderIds }
        return real.getVirtualAccountOrderId(userWalletId)
    }

    override suspend fun storeVirtualAccountOrderId(userWalletId: UserWalletId, vaOrderId: String) {
        if (isMockMode) {
            mockVaOrderIds.add(userWalletId)
            return
        }
        real.storeVirtualAccountOrderId(userWalletId, vaOrderId)
    }

    override suspend fun clearVirtualAccountOrderId(userWalletId: UserWalletId) {
        if (isMockMode) {
            mockVaOrderIds.remove(userWalletId)
            return
        }
        real.clearVirtualAccountOrderId(userWalletId)
    }

    // The "existing Tangem Pay customer" gate (decides whether an active Payment account — and accounts mode —
    // appears). Delegates to WireMock's checkCustomerWalletId via the real repo (static token, no signing), so it
    // is driven by the `tangem_pay_eligibility` scenario: `Started` (default) → 404/NotFound → no account;
    // `PaeraCustomer` → 200 → account. Generic UI tests never set the scenario, so they stay Payment-free.
    override suspend fun hasTangemPayInWallet(userWalletId: UserWalletId): Either<VisaApiError, Boolean> =
        real.hasTangemPayInWallet(userWalletId)

    override suspend fun checkCustomerEligibility(): List<TangemPayEligibilityType> =
        real.checkCustomerEligibility()

    override suspend fun getCustomerEligibility(): List<TangemPayEligibilityType>? =
        real.getCustomerEligibility()

    override suspend fun fetchCustomerEligibility(
        userWalletId: UserWalletId,
    ): Either<VisaApiError, List<TangemPayEligibilityType>> = real.fetchCustomerEligibility(userWalletId)

    override fun getSavedCustomerInfo(userWalletId: UserWalletId): CustomerInfo? =
        real.getSavedCustomerInfo(userWalletId)

    override fun isMainOnboardingBannerHidden(userWalletId: UserWalletId): Flow<Boolean> {
        if (isMockMode) return flowOf(false)
        return real.isMainOnboardingBannerHidden(userWalletId)
    }

    override suspend fun setHideMainOnboardingBanner(userWalletId: UserWalletId) {
        if (isMockMode) return
        real.setHideMainOnboardingBanner(userWalletId)
    }

    // Not short-circuited in MOCK env: the account removal is only observable through what this call does —
    // the pay-enabled request against WireMock, clearing the wallet's Tangem Pay data and storing the empty
    // payment-account status that hides the block. The synthetic order ids are part of that data, so they go
    // with it — otherwise a later refresh would keep replaying the removed account's enrollment.
    override suspend fun disableTangemPay(userWalletId: UserWalletId): Either<VisaApiError, Unit> =
        real.disableTangemPay(userWalletId).onRight {
            mockOrderIds.remove(userWalletId)
            mockVaOrderIds.remove(userWalletId)
        }

    // Follows the flag the customer-state mock writes (FORMER / all product instances DEACTIVATED), so a
    // removed account stays hidden across a restart instead of reappearing.
    override suspend fun isTangemPayDeactivated(userWalletId: UserWalletId): Boolean =
        real.isTangemPayDeactivated(userWalletId)

    private companion object {
        const val MOCK_ORDER_ID = "mock-order-id"
        const val MOCK_VA_ORDER_ID = "mock-va-order-id"
    }
}