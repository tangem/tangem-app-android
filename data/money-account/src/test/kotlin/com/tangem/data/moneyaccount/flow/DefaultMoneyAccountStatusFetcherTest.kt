package com.tangem.data.moneyaccount.flow

import arrow.core.left
import com.google.common.truth.Truth.assertThat
import arrow.core.right
import com.tangem.data.moneyaccount.store.MoneyAccountStatusesStore
import com.tangem.domain.models.StatusSource
import com.tangem.domain.models.account.Account
import com.tangem.domain.models.account.AccountStatus
import com.tangem.domain.models.account.MoneyAccountStatusValue
import com.tangem.domain.models.kyc.KycStatus
import com.tangem.domain.models.pay.TangemPayCardFrozenState
import com.tangem.domain.models.wallet.UserWalletId
import com.tangem.domain.moneyaccount.flow.MoneyAccountStatusFetcher
import com.tangem.domain.moneyaccount.repository.MoneyAccountOrderRepository
import com.tangem.domain.pay.model.CustomerInfo
import com.tangem.domain.pay.model.OrderStatus
import com.tangem.domain.pay.model.OrderStep
import com.tangem.domain.pay.model.TangemPayOrderInfo
import com.tangem.domain.pay.repository.OnboardingRepository
import com.tangem.domain.visa.error.VisaApiError
import com.tangem.utils.coroutines.TestingCoroutineDispatcherProvider
import io.mockk.clearMocks
import io.mockk.coEvery
import io.mockk.coVerify
import io.mockk.every
import io.mockk.mockk
import kotlinx.coroutines.test.runTest
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.TestInstance
import java.math.BigDecimal

@TestInstance(TestInstance.Lifecycle.PER_CLASS)
internal class DefaultMoneyAccountStatusFetcherTest {

    private val moneyAccountStatusesStore: MoneyAccountStatusesStore = mockk(relaxed = true)
    private val orderRepository: MoneyAccountOrderRepository = mockk()
    private val onboardingRepository: OnboardingRepository = mockk()

    private val fetcher = DefaultMoneyAccountStatusFetcher(
        moneyAccountStatusesStore = moneyAccountStatusesStore,
        orderRepository = orderRepository,
        onboardingRepository = onboardingRepository,
        dispatchers = TestingCoroutineDispatcherProvider(),
    )

    @BeforeEach
    fun resetMocks() {
        clearMocks(moneyAccountStatusesStore, orderRepository, onboardingRepository)
        coEvery { moneyAccountStatusesStore.getSyncOrNull(USER_WALLET_ID) } returns null
        every { orderRepository.getOrder(USER_WALLET_ID) } returns null
        coEvery { onboardingRepository.getCustomerInfo(USER_WALLET_ID) } returns createCustomerInfo().right()
    }

    @Test
    fun `GIVEN no account and no order WHEN invoke THEN not created is stored`() = runTest {
        // Act
        fetcher.invoke(MoneyAccountStatusFetcher.Params(USER_WALLET_ID))

        // Assert
        verifyStored(MoneyAccountStatusValue.NotCreated)
    }

    @Test
    fun `GIVEN customer info is unavailable WHEN invoke THEN unavailable is stored`() = runTest {
        // Arrange
        coEvery {
            onboardingRepository.getCustomerInfo(USER_WALLET_ID)
        } returns VisaApiError.ServerUnavailable.left()

        // Act
        fetcher.invoke(MoneyAccountStatusFetcher.Params(USER_WALLET_ID))

        // Assert
        verifyStored(MoneyAccountStatusValue.Error.Unavailable)
    }

    @Test
    fun `GIVEN active product instance WHEN invoke THEN active account is stored`() = runTest {
        // Arrange
        coEvery { onboardingRepository.getCustomerInfo(USER_WALLET_ID) } returns createCustomerInfo(
            accountStatus = CustomerInfo.ProductInstance.Status.ACTIVE,
        ).right()

        // Act
        fetcher.invoke(MoneyAccountStatusFetcher.Params(USER_WALLET_ID))

        // Assert
        verifyStored(
            MoneyAccountStatusValue.Account.Active(
                source = StatusSource.ACTUAL,
                balance = null,
                customerId = CUSTOMER_ID,
                productInstanceId = PRODUCT_INSTANCE_ID,
            ),
        )
    }

    @Test
    fun `GIVEN order is being issued WHEN invoke THEN issuing is stored`() = runTest {
        // Arrange
        every { orderRepository.getOrder(USER_WALLET_ID) } returns createOrder(step = OrderStep.ELIGIBILITY_CHECK)

        // Act
        fetcher.invoke(MoneyAccountStatusFetcher.Params(USER_WALLET_ID))

        // Assert
        verifyStored(
            MoneyAccountStatusValue.Onboarding.Issuing(
                source = StatusSource.ACTUAL,
                balance = null,
                customerId = CUSTOMER_ID,
                step = MoneyAccountStatusValue.IssuingStep.ELIGIBILITY_CHECK,
            ),
        )
    }

    @Test
    fun `GIVEN order awaits verification WHEN invoke THEN under review is stored`() = runTest {
        // Arrange
        every { orderRepository.getOrder(USER_WALLET_ID) } returns createOrder(step = OrderStep.AWAITING_KYC)

        // Act
        fetcher.invoke(MoneyAccountStatusFetcher.Params(USER_WALLET_ID))

        // Assert
        verifyStored(
            MoneyAccountStatusValue.Onboarding.UnderReview(
                source = StatusSource.ACTUAL,
                balance = null,
                customerId = CUSTOMER_ID,
                kycStatus = KycStatus.APPROVED,
            ),
        )
    }

    @Test
    fun `GIVEN verification is rejected WHEN invoke THEN declined is stored`() = runTest {
        // Arrange
        every { orderRepository.getOrder(USER_WALLET_ID) } returns createOrder(step = OrderStep.AWAITING_KYC)
        coEvery { onboardingRepository.getCustomerInfo(USER_WALLET_ID) } returns createCustomerInfo(
            kycStatus = KycStatus.REJECTED,
        ).right()

        // Act
        fetcher.invoke(MoneyAccountStatusFetcher.Params(USER_WALLET_ID))

        // Assert
        verifyStored(
            MoneyAccountStatusValue.Onboarding.VerificationDeclined(
                source = StatusSource.ACTUAL,
                balance = null,
                customerId = CUSTOMER_ID,
            ),
        )
    }

    @Test
    fun `GIVEN order is completed WHEN invoke THEN activating is stored`() = runTest {
        // Arrange
        every { orderRepository.getOrder(USER_WALLET_ID) } returns createOrder(status = OrderStatus.COMPLETED)

        // Act
        fetcher.invoke(MoneyAccountStatusFetcher.Params(USER_WALLET_ID))

        // Assert
        verifyStored(
            MoneyAccountStatusValue.Onboarding.Activating(
                source = StatusSource.ACTUAL,
                balance = null,
                customerId = CUSTOMER_ID,
            ),
        )
    }

    @Test
    fun `GIVEN order is gone but onboarding was in progress WHEN invoke THEN last status is kept`() = runTest {
        // Arrange
        val previous = MoneyAccountStatusValue.Onboarding.Activating(
            source = StatusSource.ACTUAL,
            balance = null,
            customerId = CUSTOMER_ID,
        )
        coEvery { moneyAccountStatusesStore.getSyncOrNull(USER_WALLET_ID) } returns AccountStatus.Money(
            account = Account.Money(userWalletId = USER_WALLET_ID),
            value = previous,
        )

        // Act
        fetcher.invoke(MoneyAccountStatusFetcher.Params(USER_WALLET_ID))

        // Assert
        verifyStored(previous)
    }

    @Test
    fun `GIVEN the store fails WHEN invoke THEN the error is returned instead of being thrown`() = runTest {
        // Arrange
        coEvery { moneyAccountStatusesStore.store(any(), any()) } throws IllegalStateException()

        // Act
        val actual = fetcher.invoke(MoneyAccountStatusFetcher.Params(USER_WALLET_ID))

        // Assert
        assertThat(actual.isLeft()).isTrue()
    }

    private suspend fun verifyStored(value: MoneyAccountStatusValue) {
        coVerify(exactly = 1) {
            moneyAccountStatusesStore.store(
                userWalletId = USER_WALLET_ID,
                status = AccountStatus.Money(
                    account = Account.Money(userWalletId = USER_WALLET_ID),
                    value = value,
                ),
            )
        }
    }

    private fun createOrder(
        status: OrderStatus = OrderStatus.PROCESSING,
        step: OrderStep = OrderStep.STARTING,
    ) = TangemPayOrderInfo(
        orderId = "order-1",
        orderStatus = status,
        orderStep = step,
    )

    private fun createCustomerInfo(
        kycStatus: KycStatus = KycStatus.APPROVED,
        accountStatus: CustomerInfo.ProductInstance.Status? = null,
    ) = CustomerInfo(
        customerId = CUSTOMER_ID,
        paymentAccount = null,
        productInstances = listOfNotNull(accountStatus?.let(::createAccountInstance)),
        cards = emptyList(),
        kycStatus = kycStatus,
        state = CustomerInfo.State.ACTIVE,
        fiatBalance = null,
        cryptoBalance = null,
        availableForWithdrawal = BigDecimal.ZERO,
        tariffPlan = null,
    )

    private fun createAccountInstance(status: CustomerInfo.ProductInstance.Status) = CustomerInfo.ProductInstance(
        id = PRODUCT_INSTANCE_ID,
        cardId = "",
        frozenState = TangemPayCardFrozenState.Unfrozen,
        displayName = null,
        actualCardLimit = null,
        adminCardLimit = null,
        status = status,
        specificationDataType = CustomerInfo.ProductInstance.SpecificationDataType.ACCOUNT,
    )

    private companion object {
        val USER_WALLET_ID = UserWalletId("011")
        const val CUSTOMER_ID = "customer-1"
        const val PRODUCT_INSTANCE_ID = "pi-1"
    }
}