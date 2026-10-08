package com.tangem.domain.moneyaccount.usecase

import arrow.core.left
import arrow.core.right
import com.tangem.domain.models.wallet.UserWalletId
import com.tangem.domain.moneyaccount.flow.MoneyAccountStatusFetcher
import com.tangem.domain.moneyaccount.repository.MoneyAccountOrderRepository
import com.tangem.domain.pay.model.Order
import com.tangem.domain.pay.model.OrderStatus
import com.tangem.domain.pay.model.OrderStep
import com.tangem.domain.pay.model.OrderType
import com.tangem.domain.pay.model.TangemPayOrderInfo
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
    private val trackOrder: TrackMoneyAccountOrderUseCase = mockk()
    private val findMoneyAccountCreationOrderUseCase: FindMoneyAccountCreationOrderUseCase = mockk()
    private val moneyAccountStatusFetcher: MoneyAccountStatusFetcher = mockk(relaxed = true)

    private val useCase = RestoreMoneyAccountOrderUseCase(
        trackOrder = trackOrder,
        findMoneyAccountCreationOrderUseCase = findMoneyAccountCreationOrderUseCase,
        moneyAccountOrderRepository = orderRepository,
        moneyAccountStatusFetcher = moneyAccountStatusFetcher,
    )

    @BeforeEach
    fun resetMocks() {
        clearMocks(orderRepository, trackOrder, findMoneyAccountCreationOrderUseCase, moneyAccountStatusFetcher)
        every { orderRepository.storeOrder(any(), any()) } just runs
        every { orderRepository.getOrder(USER_WALLET_ID) } returns null
        every { trackOrder(any(), any()) } just runs
        coEvery { findMoneyAccountCreationOrderUseCase(USER_WALLET_ID) } returns null.right()
    }

    @Test
    fun `GIVEN an active order WHEN invoke THEN tracking is started once`() = runTest {
        // Arrange
        val order = createOrder()
        coEvery { findMoneyAccountCreationOrderUseCase(USER_WALLET_ID) } returns order.right()

        // Act
        useCase(USER_WALLET_ID)

        // Assert
        verify(exactly = 1) { trackOrder(USER_WALLET_ID, TangemPayOrderInfo.fromOrder(order)) }
        coVerify(exactly = 1) { findMoneyAccountCreationOrderUseCase(USER_WALLET_ID) }
    }

    @Test
    fun `GIVEN no order WHEN invoke THEN empty order is stored and status is fetched once`() = runTest {
        // Act
        useCase(USER_WALLET_ID)

        // Assert
        verify(exactly = 1) { orderRepository.storeOrder(USER_WALLET_ID, null.right()) }
        coVerify(exactly = 1) { moneyAccountStatusFetcher.invoke(USER_WALLET_ID) }
        coVerify(exactly = 1) { findMoneyAccountCreationOrderUseCase(USER_WALLET_ID) }
        verify(exactly = 0) { trackOrder(any(), any()) }
    }

    @Test
    fun `GIVEN find fails once WHEN invoke THEN error is stored and request is retried`() = runTest {
        // Arrange
        val order = createOrder()
        coEvery { findMoneyAccountCreationOrderUseCase(USER_WALLET_ID) } returnsMany listOf(
            VisaApiError.ServerUnavailable.left(),
            order.right(),
        )

        // Act
        useCase(USER_WALLET_ID)

        // Assert
        verify(exactly = 1) { orderRepository.storeOrder(USER_WALLET_ID, VisaApiError.ServerUnavailable.left()) }
        coVerify(exactly = 2) { findMoneyAccountCreationOrderUseCase(USER_WALLET_ID) }
        verify(exactly = 1) { trackOrder(USER_WALLET_ID, TangemPayOrderInfo.fromOrder(order)) }
    }

    @Test
    fun `GIVEN order is already known WHEN find fails THEN known order is not overwritten`() = runTest {
        // Arrange
        val order = createOrder()
        every { orderRepository.getOrder(USER_WALLET_ID) } returns TangemPayOrderInfo.fromOrder(order).right()
        coEvery { findMoneyAccountCreationOrderUseCase(USER_WALLET_ID) } returnsMany listOf(
            VisaApiError.ServerUnavailable.left(),
            order.right(),
        )

        // Act
        useCase(USER_WALLET_ID)

        // Assert
        verify(exactly = 0) { orderRepository.storeOrder(USER_WALLET_ID, VisaApiError.ServerUnavailable.left()) }
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

    private companion object {
        val USER_WALLET_ID = UserWalletId("011")
        const val ORDER_ID = "order-1"
    }
}