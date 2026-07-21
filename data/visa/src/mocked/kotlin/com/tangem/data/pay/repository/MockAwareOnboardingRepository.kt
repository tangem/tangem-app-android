package com.tangem.data.pay.repository

import arrow.core.Either
import arrow.core.right
import com.tangem.core.error.UniversalError
import com.tangem.data.pay.TangemPayMockControl
import com.tangem.datasource.api.common.config.ApiConfig
import com.tangem.datasource.api.common.config.ApiEnvironment
import com.tangem.datasource.api.common.config.managers.ApiConfigsManager
import com.tangem.domain.models.account.BankCredentials
import com.tangem.domain.models.pay.TangemPayEligibilityType
import com.tangem.domain.models.wallet.UserWalletId
import com.tangem.domain.pay.model.CustomerInfo
import com.tangem.domain.pay.repository.OnboardingRepository
import com.tangem.domain.visa.error.VisaApiError
import java.util.concurrent.ConcurrentHashMap
import javax.inject.Inject
import javax.inject.Singleton

/**
 * In MOCK env only skips the local-storage / NFC-signing enrollment steps (order ids, initial data). The
 * customer-facing state — whether a wallet has Tangem Pay ([hasTangemPayInWallet]), KYC status, ACTIVE /
 * INACTIVE, balances ([getCustomerInfo]) — is driven by the WireMock test scenario (authenticated with the
 * synthetic tokens from [com.tangem.data.pay.store.MockAwareTangemPayStorage]) rather than hardcoded:
 *  - [hasTangemPayInWallet] is answered locally via [TangemPayMockControl] (WireMock does not mock the
 *    underlying checkCustomerWalletId endpoint) — default false, opt-in true for Tangem Pay scenarios;
 *  - [getCustomerInfo] delegates to the real repo (WireMock), so KYC / customer-state scenarios take effect.
 */
@Singleton
internal class MockAwareOnboardingRepository @Inject constructor(
    private val real: DefaultOnboardingRepository,
    private val apiConfigsManager: ApiConfigsManager,
) : OnboardingRepository {

    private val mockOrderIds: MutableSet<UserWalletId> = ConcurrentHashMap.newKeySet()
    private val mockVaOrderIds: MutableSet<UserWalletId> = ConcurrentHashMap.newKeySet()

    private val isMockMode: Boolean
        get() = apiConfigsManager
            .getEnvironmentConfig(ApiConfig.ID.TangemPay)
            .environment == ApiEnvironment.MOCK

    override suspend fun validateDeeplink(link: String): Either<UniversalError, Boolean> {
        if (isMockMode) return true.right()
        return real.validateDeeplink(link)
    }

    override suspend fun isTangemPayInitialDataProduced(userWalletId: UserWalletId): Boolean {
        if (isMockMode) return true
        return real.isTangemPayInitialDataProduced(userWalletId)
    }

    override suspend fun produceInitialData(userWalletId: UserWalletId) {
        if (isMockMode) return
        real.produceInitialData(userWalletId)
    }

    // Delegates to WireMock (via the real repo + synthetic storage tokens) so the customer state — KYC status,
    // ACTIVE/INACTIVE, balances — follows the test scenario instead of a hardcoded "always active" customer.
    // Only reached when hasTangemPayInWallet is true, i.e. for wallets opted in via TangemPayMockControl.
    override suspend fun getCustomerInfo(userWalletId: UserWalletId): Either<VisaApiError, CustomerInfo> =
        real.getCustomerInfo(userWalletId)

    override suspend fun getBankCredentials(
        userWalletId: UserWalletId,
        productInstanceId: String,
    ): Either<VisaApiError, BankCredentials> = real.getBankCredentials(userWalletId, productInstanceId)

    override suspend fun createOrder(userWalletId: UserWalletId): Either<VisaApiError, String> {
        if (isMockMode) {
            mockOrderIds.add(userWalletId)
            return MOCK_ORDER_ID.right()
        }
        return real.createOrder(userWalletId)
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

    // This is the gate that decides whether a wallet is treated as an existing Tangem Pay customer (and thus
    // whether an active Payment account — and accounts mode — appears). WireMock does NOT mock the underlying
    // checkCustomerWalletId endpoint, so we answer locally: default false (generic UI tests stay Payment-free),
    // opt-in true via TangemPayMockControl for Tangem Pay scenarios. Never delegate to `real` here.
    override suspend fun hasTangemPayInWallet(userWalletId: UserWalletId): Either<VisaApiError, Boolean> {
        if (isMockMode) return TangemPayMockControl.hasTangemPayInWallet.right()
        return real.hasTangemPayInWallet(userWalletId)
    }

    override suspend fun checkCustomerEligibility(): List<TangemPayEligibilityType> =
        real.checkCustomerEligibility()

    override suspend fun getCustomerEligibility(): List<TangemPayEligibilityType> =
        real.getCustomerEligibility()

    override suspend fun fetchCustomerEligibility(
        userWalletId: UserWalletId,
    ): Either<VisaApiError, List<TangemPayEligibilityType>> = real.fetchCustomerEligibility(userWalletId)

    override fun getSavedCustomerInfo(userWalletId: UserWalletId): CustomerInfo? =
        real.getSavedCustomerInfo(userWalletId)

    override suspend fun getHideMainOnboardingBanner(userWalletId: UserWalletId): Boolean {
        if (isMockMode) return false
        return real.getHideMainOnboardingBanner(userWalletId)
    }

    override suspend fun setHideMainOnboardingBanner(userWalletId: UserWalletId) {
        if (isMockMode) return
        real.setHideMainOnboardingBanner(userWalletId)
    }

    override suspend fun disableTangemPay(userWalletId: UserWalletId): Either<VisaApiError, Unit> {
        if (isMockMode) return Unit.right()
        return real.disableTangemPay(userWalletId)
    }

    override suspend fun isTangemPayDeactivated(userWalletId: UserWalletId): Boolean {
        if (isMockMode) return false
        return real.isTangemPayDeactivated(userWalletId)
    }

    private companion object {
        const val MOCK_ORDER_ID = "mock-order-id"
        const val MOCK_VA_ORDER_ID = "mock-va-order-id"
    }
}