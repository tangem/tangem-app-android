package com.tangem.data.pay.repository

import arrow.core.Either
import arrow.core.right
import com.tangem.core.error.UniversalError
import com.tangem.datasource.api.common.config.ApiConfig
import com.tangem.datasource.api.common.config.ApiEnvironment
import com.tangem.datasource.api.common.config.managers.ApiConfigsManager
import com.tangem.domain.models.account.BankCredentials
import com.tangem.domain.models.account.PaymentAccountStatusValue
import com.tangem.domain.models.kyc.KycStatus
import com.tangem.domain.models.pay.TangemPayCard
import com.tangem.domain.models.pay.TangemPayCardFrozenState
import com.tangem.domain.models.pay.TangemPayEligibilityType
import com.tangem.domain.models.wallet.UserWalletId
import com.tangem.domain.pay.model.CustomerInfo
import com.tangem.domain.pay.repository.OnboardingRepository
import com.tangem.domain.visa.error.VisaApiError
import java.math.BigDecimal
import java.util.concurrent.ConcurrentHashMap
import javax.inject.Inject
import javax.inject.Singleton

/**
 * In MOCK env returns canned onboarding data so the Payment Account shows up as fully loaded on the main
 * screen (the entry point to the TangemPay details & cashback screens), and skips local-storage / signing
 * enrollment; remaining server calls go to WireMock.
 */
@Singleton
internal class MockAwareOnboardingRepository @Inject constructor(
    private val real: DefaultOnboardingRepository,
    private val apiConfigsManager: ApiConfigsManager,
    private val cardNameHolder: MockTangemPayCardNameHolder,
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

    override suspend fun getCustomerInfo(userWalletId: UserWalletId): Either<VisaApiError, CustomerInfo> {
        if (isMockMode) return mockCustomerInfo().right()
        return real.getCustomerInfo(userWalletId)
    }

    private fun mockCustomerInfo(): CustomerInfo = MOCK_CUSTOMER_INFO.copy(
        productInstances = MOCK_CUSTOMER_INFO.productInstances.map {
            it.copy(displayName = cardNameHolder.displayName)
        },
    )

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

    override suspend fun hasTangemPayInWallet(userWalletId: UserWalletId): Either<VisaApiError, Boolean> {
        if (isMockMode) return true.right()
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

        const val MOCK_CUSTOMER_ID = "mock-customer-id"
        const val MOCK_CARD_ID = "mock-card-id"
        const val MOCK_PRODUCT_INSTANCE_ID = "mock-product-instance-id"
        const val MOCK_CUSTOMER_WALLET_ADDRESS = "0x0000000000000000000000000000000000000002"
        const val MOCK_TOKEN_CONTRACT_ADDRESS = "0x3c499c542cef5e3811e1192ce70d8cc03d5c3359"
        const val MOCK_POLYGON_CHAIN_ID = 137L

        val MOCK_CUSTOMER_INFO = CustomerInfo(
            customerId = MOCK_CUSTOMER_ID,
            productInstances = listOf(
                CustomerInfo.ProductInstance(
                    id = MOCK_PRODUCT_INSTANCE_ID,
                    cardId = MOCK_CARD_ID,
                    frozenState = TangemPayCardFrozenState.Unfrozen,
                    displayName = null,
                    actualCardLimit = null,
                    adminCardLimit = null,
                    status = CustomerInfo.ProductInstance.Status.ACTIVE,
                    specificationDataType = CustomerInfo.ProductInstance.SpecificationDataType.CARD,
                ),
            ),
            cards = listOf(
                CustomerInfo.CardInfo(
                    cardId = MOCK_CARD_ID,
                    cardStatus = TangemPayCard.Status.ACTIVE,
                    lastFourDigits = "4242",
                    isPinSet = true,
                    images = emptyList(),
                ),
            ),
            kycStatus = KycStatus.APPROVED,
            state = CustomerInfo.State.ACTIVE,
            fiatBalance = PaymentAccountStatusValue.FiatBalance(
                availableBalance = BigDecimal("123.45"),
                currency = "USD",
            ),
            cryptoBalance = PaymentAccountStatusValue.CryptoBalance(
                id = "usd-coin",
                chainId = MOCK_POLYGON_CHAIN_ID,
                depositAddress = MOCK_CUSTOMER_WALLET_ADDRESS,
                tokenContractAddress = MOCK_TOKEN_CONTRACT_ADDRESS,
                balance = BigDecimal("123.45"),
            ),
            availableForWithdrawal = BigDecimal("123.45"),
            tariffPlan = null,
        )
    }
}