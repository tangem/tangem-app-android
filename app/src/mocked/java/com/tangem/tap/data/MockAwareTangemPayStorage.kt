package com.tangem.tap.data

import com.tangem.datasource.api.common.config.ApiConfig
import com.tangem.datasource.api.common.config.ApiEnvironment
import com.tangem.datasource.api.common.config.managers.ApiConfigsManager
import com.tangem.datasource.local.visa.TangemPayStorage
import com.tangem.domain.models.wallet.UserWalletId
import com.tangem.domain.pay.TangemPayWithdrawState
import com.tangem.domain.visa.model.TangemPayAuthTokens
import javax.inject.Inject
import javax.inject.Singleton

private const val MOCK_CUSTOMER_WALLET_ADDRESS = "0x0000000000000000000000000000000000000002"
private const val MOCK_ACCESS_TOKEN = "mock-access-token"
private const val MOCK_REFRESH_TOKEN = "mock-refresh-token"
private const val MOCK_IDEMPOTENCY_KEY = "mock-idempotency-key"
private const val MOCK_TOKEN_EXPIRES_AT = 9_999_999_999L

/** In MOCK env returns synthetic auth tokens + customer wallet address; otherwise delegates. */
@Singleton
internal class MockAwareTangemPayStorage @Inject constructor(
    private val real: DefaultTangemPayStorage,
    private val apiConfigsManager: ApiConfigsManager,
) : TangemPayStorage {

    private val isMockMode: Boolean
        get() = apiConfigsManager
            .getEnvironmentConfig(ApiConfig.ID.TangemPay)
            .environment == ApiEnvironment.MOCK

    override suspend fun storeCustomerWalletAddress(userWalletId: UserWalletId, customerWalletAddress: String) {
        if (isMockMode) return
        real.storeCustomerWalletAddress(userWalletId, customerWalletAddress)
    }

    override suspend fun getCustomerWalletAddress(userWalletId: UserWalletId): String? {
        if (isMockMode) return MOCK_CUSTOMER_WALLET_ADDRESS
        return real.getCustomerWalletAddress(userWalletId)
    }

    override suspend fun clearCustomerWalletAddress(userWalletId: UserWalletId) {
        if (isMockMode) return
        real.clearCustomerWalletAddress(userWalletId)
    }

    override suspend fun storeAuthTokens(customerWalletAddress: String, tokens: TangemPayAuthTokens) {
        if (isMockMode) return
        real.storeAuthTokens(customerWalletAddress, tokens)
    }

    override suspend fun getAuthTokens(customerWalletAddress: String): TangemPayAuthTokens? {
        if (isMockMode) {
            return TangemPayAuthTokens(
                accessToken = MOCK_ACCESS_TOKEN,
                expiresAt = MOCK_TOKEN_EXPIRES_AT,
                refreshToken = MOCK_REFRESH_TOKEN,
                refreshExpiresAt = MOCK_TOKEN_EXPIRES_AT,
                idempotencyKey = MOCK_IDEMPOTENCY_KEY,
            )
        }
        return real.getAuthTokens(customerWalletAddress)
    }

    override suspend fun clearAuthTokens(customerWalletAddress: String) {
        if (isMockMode) return
        real.clearAuthTokens(customerWalletAddress)
    }

    override suspend fun storeOrderId(customerWalletAddress: String, orderId: String) =
        real.storeOrderId(customerWalletAddress, orderId)

    override suspend fun getOrderId(customerWalletAddress: String): String? =
        real.getOrderId(customerWalletAddress)

    override suspend fun getAddToWalletDone(customerWalletAddress: String): Boolean =
        real.getAddToWalletDone(customerWalletAddress)

    override suspend fun storeAddToWalletDone(customerWalletAddress: String, isDone: Boolean) =
        real.storeAddToWalletDone(customerWalletAddress, isDone)

    override suspend fun clearOrderId(customerWalletAddress: String) =
        real.clearOrderId(customerWalletAddress)

    override suspend fun storeCheckCustomerWalletResult(userWalletId: UserWalletId, isPaeraCustomer: Boolean) =
        real.storeCheckCustomerWalletResult(userWalletId, isPaeraCustomer)

    override suspend fun checkCustomerWalletResult(userWalletId: UserWalletId): Boolean? {
        if (isMockMode) return true
        return real.checkCustomerWalletResult(userWalletId)
    }

    override suspend fun storeActiveWithdrawOrderId(userWalletId: UserWalletId, orderId: String) =
        real.storeActiveWithdrawOrderId(userWalletId, orderId)

    override suspend fun storeWithdrawOrder(userWalletId: UserWalletId, data: TangemPayWithdrawState) =
        real.storeWithdrawOrder(userWalletId, data)

    override suspend fun getActiveWithdrawOrderId(userWalletId: UserWalletId): String? =
        real.getActiveWithdrawOrderId(userWalletId)

    override suspend fun getWithdrawOrders(userWalletId: UserWalletId): List<TangemPayWithdrawState>? =
        real.getWithdrawOrders(userWalletId)

    override suspend fun deleteActiveWithdrawOrder(userWalletId: UserWalletId) =
        real.deleteActiveWithdrawOrder(userWalletId)

    override suspend fun deleteWithdrawOrder(userWalletId: UserWalletId, orderId: String) =
        real.deleteWithdrawOrder(userWalletId, orderId)

    override suspend fun storeHideOnboardingBanner(userWalletId: UserWalletId, hide: Boolean) =
        real.storeHideOnboardingBanner(userWalletId, hide)

    override suspend fun getHideMainOnboardingBanner(userWalletId: UserWalletId): Boolean =
        real.getHideMainOnboardingBanner(userWalletId)

    override suspend fun storeTangemPayEligibility(eligibility: Set<String>) =
        real.storeTangemPayEligibility(eligibility)

    override suspend fun getTangemPayEligibility(): Set<String> = real.getTangemPayEligibility()

    override suspend fun storeIsTangemPayDeactivated(userWalletId: UserWalletId) =
        real.storeIsTangemPayDeactivated(userWalletId)

    override suspend fun isTangemPayDeactivated(userWalletId: UserWalletId): Boolean =
        real.isTangemPayDeactivated(userWalletId)

    override suspend fun clearAll(userWalletId: UserWalletId, customerWalletAddress: String) =
        real.clearAll(userWalletId, customerWalletAddress)
}