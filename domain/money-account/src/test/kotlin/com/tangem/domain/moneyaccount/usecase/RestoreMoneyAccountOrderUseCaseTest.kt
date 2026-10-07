package com.tangem.domain.moneyaccount.usecase

import arrow.core.left
import arrow.core.right
import com.tangem.domain.models.kyc.KycStatus
import com.tangem.domain.models.pay.TangemPayCardFrozenState
import com.tangem.domain.models.wallet.UserWalletId
import com.tangem.domain.moneyaccount.repository.MoneyAccountOrderRepository
import com.tangem.domain.pay.model.CustomerInfo
import com.tangem.domain.pay.model.Order
import com.tangem.domain.pay.model.OrderStatus
import com.tangem.domain.pay.model.OrderStep
import com.tangem.domain.pay.model.OrderType
import com.tangem.domain.pay.model.TangemPayOrderInfo
import com.tangem.domain.pay.repository.OnboardingRepository
import com.tangem.domain.pay.usecase.FindMoneyAccountCreationOrderUseCase
import com.tangem.domain.visa.error.VisaApiError
import io.mockk.clearMocks
import io.mockk.coEvery
import io.mockk.coVerify
import io.mockk.every
import io.mockk.just
import io.mockk.mockk
import io.mockk.runs
import io.mockk.verify
import kotlinx.coroutines.test.runTest
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.TestInstance

@TestInstance(TestInstance.Lifecycle.PER_CLASS)
internal class RestoreMoneyAccountOrderUseCaseTest {

    private val orderRepository: MoneyAccountOrderRepository = mockk()
    private val onboardingRepository: OnboardingRepository = mockk()
    private val trackOrder: TrackMoneyAccountOrderUseCase = mockk()
    private val findMoneyAccountCreationOrderUseCase: FindMoneyAccountCreationOrderUseCase = mockk()

    private val useCase = RestoreMoneyAccountOrderUseCase(
        orderRepository = orderRepository,
        onboardingRepository = onboardingRepository,
        trackOrder = trackOrder,
        findMoneyAccountCreationOrderUseCase = findMoneyAccountCreationOrderUseCase,
    )

    @BeforeEach
    fun resetMocks() {
        clearMocks(orderRepository, onboardingRepository, trackOrder, findMoneyAccountCreationOrderUseCase)
        every { orderRepository.clearOrder(USER_WALLET_ID) } just runs
        every { trackOrder(any(), any()) } just runs
        coEvery { onboardingRepository.getCustomerInfo(USER_WALLET_ID) } returns createCustomerInfo().right()
        coEvery { findMoneyAccountCreationOrderUseCase(USER_WALLET_ID) } returns null.right()
    }

    @Test
    fun `GIVEN customer info is unavailable WHEN invoke THEN nothing is tracked or cleared`() = runTest {
        // Arrange
        coEvery {
            onboardingRepository.getCustomerInfo(USER_WALLET_ID)
        } returns VisaApiError.ServerUnavailable.left()

        // Act
        useCase(USER_WALLET_ID)

        // Assert
        verify(exactly = 0) { trackOrder(any(), any()) }
        verify(exactly = 0) { orderRepository.clearOrder(any()) }
        coVerify(exactly = 0) { findMoneyAccountCreationOrderUseCase(any()) }
    }

    @Test
    fun `GIVEN account is active WHEN invoke THEN order is cleared and nothing is tracked`() = runTest {
        // Arrange
        coEvery { onboardingRepository.getCustomerInfo(USER_WALLET_ID) } returns createCustomerInfo(
            accountStatus = CustomerInfo.ProductInstance.Status.ACTIVE,
        ).right()

        // Act
        useCase(USER_WALLET_ID)

        // Assert
        verify(exactly = 1) { orderRepository.clearOrder(USER_WALLET_ID) }
        verify(exactly = 0) { trackOrder(any(), any()) }
    }

    @Test
    fun `GIVEN account is deactivated WHEN invoke THEN order is cleared`() = runTest {
        // Arrange
        coEvery { onboardingRepository.getCustomerInfo(USER_WALLET_ID) } returns createCustomerInfo(
            accountStatus = CustomerInfo.ProductInstance.Status.DEACTIVATED,
        ).right()

        // Act
        useCase(USER_WALLET_ID)

        // Assert
        verify(exactly = 1) { orderRepository.clearOrder(USER_WALLET_ID) }
        verify(exactly = 0) { trackOrder(any(), any()) }
    }

    @Test
    fun `GIVEN customer is former WHEN invoke THEN order is cleared`() = runTest {
        // Arrange
        coEvery { onboardingRepository.getCustomerInfo(USER_WALLET_ID) } returns createCustomerInfo(
            state = CustomerInfo.State.FORMER,
        ).right()

        // Act
        useCase(USER_WALLET_ID)

        // Assert
        verify(exactly = 1) { orderRepository.clearOrder(USER_WALLET_ID) }
        verify(exactly = 0) { trackOrder(any(), any()) }
    }

    @Test
    fun `GIVEN an active order WHEN invoke THEN tracking is started`() = runTest {
        // Arrange
        val order = createOrder()
        coEvery { findMoneyAccountCreationOrderUseCase(USER_WALLET_ID) } returns order.right()

        // Act
        useCase(USER_WALLET_ID)

        // Assert
        verify(exactly = 1) { trackOrder(USER_WALLET_ID, TangemPayOrderInfo.fromOrder(order)) }
        verify(exactly = 0) { orderRepository.clearOrder(any()) }
    }

    @Test
    fun `GIVEN no active order WHEN invoke THEN nothing is tracked`() = runTest {
        // Act
        useCase(USER_WALLET_ID)

        // Assert
        verify(exactly = 0) { trackOrder(any(), any()) }
    }

    private fun createOrder() = Order(
        id = ORDER_ID,
        customerId = null,
        type = OrderType.ACCOUNT_ISSUE_BANKING_RAIN,
        status = OrderStatus.PROCESSING,
        step = OrderStep.AWAITING_KYC,
        stepChangeCode = null,
        productInstanceId = null,
        paymentAccountId = null,
        cardId = null,
        toTariffPlanId = null,
        withdrawTxHash = null,
        createdAt = null,
        updatedAt = null,
    )

    private fun createCustomerInfo(
        state: CustomerInfo.State = CustomerInfo.State.ACTIVE,
        accountStatus: CustomerInfo.ProductInstance.Status? = null,
    ) = CustomerInfo(
        customerId = CUSTOMER_ID,
        paymentAccount = null,
        productInstances = listOfNotNull(accountStatus?.let(::createAccountInstance)),
        cards = emptyList(),
        kycStatus = KycStatus.APPROVED,
        state = state,
        fiatBalance = null,
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
        paymentAccountType = CustomerInfo.ProductInstance.PaymentAccountType.COLLATERAL_BANKING_ACCOUNT,
    )

    private companion object {
        val USER_WALLET_ID = UserWalletId("011")
        const val ORDER_ID = "order-1"
        const val CUSTOMER_ID = "customer-1"
        const val PRODUCT_INSTANCE_ID = "pi-1"
    }
}