package com.tangem.domain.moneyaccount.usecase

import arrow.core.left
import arrow.core.right
import com.google.common.truth.Truth.assertThat
import com.tangem.domain.models.wallet.UserWalletId
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
internal class CreateMoneyAccountOrderUseCaseTest {

    private val orderRepository: MoneyAccountOrderRepository = mockk()
    private val trackOrder: TrackMoneyAccountOrderUseCase = mockk()
    private val findMoneyAccountCreationOrderUseCase: FindMoneyAccountCreationOrderUseCase = mockk()

    private val useCase = CreateMoneyAccountOrderUseCase(
        orderRepository = orderRepository,
        trackOrder = trackOrder,
        findMoneyAccountCreationOrderUseCase = findMoneyAccountCreationOrderUseCase,
    )

    @BeforeEach
    fun resetMocks() {
        clearMocks(orderRepository, trackOrder, findMoneyAccountCreationOrderUseCase)
        every { orderRepository.getOrder(USER_WALLET_ID) } returns null
        every { trackOrder(any(), any()) } just runs
        coEvery { findMoneyAccountCreationOrderUseCase(USER_WALLET_ID) } returns null.right()
    }

    @Test
    fun `GIVEN order is already tracked WHEN invoke THEN backend is not called`() = runTest {
        // Arrange
        every { orderRepository.getOrder(USER_WALLET_ID) } returns createOrderInfo()

        // Act
        val actual = invokeUseCase()

        // Assert
        assertThat(actual).isEqualTo(Unit.right())
        coVerify(exactly = 0) { findMoneyAccountCreationOrderUseCase(any()) }
        coVerify(exactly = 0) { orderRepository.createOrder(any(), any(), any()) }
    }

    @Test
    fun `GIVEN active order on backend WHEN invoke THEN it is tracked and no order is created`() = runTest {
        // Arrange
        val order = createOrder()
        coEvery { findMoneyAccountCreationOrderUseCase(USER_WALLET_ID) } returns order.right()

        // Act
        val actual = invokeUseCase()

        // Assert
        assertThat(actual).isEqualTo(Unit.right())
        verify(exactly = 1) { trackOrder(USER_WALLET_ID, TangemPayOrderInfo.fromOrder(order)) }
        coVerify(exactly = 0) { orderRepository.createOrder(any(), any(), any()) }
    }

    @Test
    fun `GIVEN no order exists WHEN invoke THEN order is created and tracked`() = runTest {
        // Arrange
        val order = createOrder()
        coEvery {
            orderRepository.createOrder(USER_WALLET_ID, DEPOSIT_ADDRESS, SIGNATURE)
        } returns order.right()

        // Act
        val actual = invokeUseCase()

        // Assert
        assertThat(actual).isEqualTo(Unit.right())
        coVerify(exactly = 1) { orderRepository.createOrder(USER_WALLET_ID, DEPOSIT_ADDRESS, SIGNATURE) }
        verify(exactly = 1) { trackOrder(USER_WALLET_ID, TangemPayOrderInfo.fromOrder(order)) }
    }

    @Test
    fun `GIVEN backend reports an active order WHEN invoke THEN the existing order is tracked`() = runTest {
        // Arrange
        val order = createOrder()
        coEvery {
            orderRepository.createOrder(USER_WALLET_ID, DEPOSIT_ADDRESS, SIGNATURE)
        } returns VisaApiError.AccountIssueActiveOrderExists.left()
        coEvery { findMoneyAccountCreationOrderUseCase(USER_WALLET_ID) } returnsMany listOf(
            null.right(),
            order.right(),
        )

        // Act
        val actual = invokeUseCase()

        // Assert
        assertThat(actual).isEqualTo(Unit.right())
        verify(exactly = 1) { trackOrder(USER_WALLET_ID, TangemPayOrderInfo.fromOrder(order)) }
    }

    @Test
    fun `GIVEN conflict without an active order WHEN invoke THEN order is not found`() = runTest {
        // Arrange
        coEvery {
            orderRepository.createOrder(USER_WALLET_ID, DEPOSIT_ADDRESS, SIGNATURE)
        } returns VisaApiError.AccountIssueActiveOrderExists.left()

        // Act
        val actual = invokeUseCase()

        // Assert
        assertThat(actual).isEqualTo(VisaApiError.OrderNotFound.left())
        verify(exactly = 0) { trackOrder(any(), any()) }
    }

    @Test
    fun `GIVEN create fails WHEN invoke THEN error is returned and nothing is tracked`() = runTest {
        // Arrange
        coEvery {
            orderRepository.createOrder(USER_WALLET_ID, DEPOSIT_ADDRESS, SIGNATURE)
        } returns VisaApiError.ServerUnavailable.left()

        // Act
        val actual = invokeUseCase()

        // Assert
        assertThat(actual).isEqualTo(VisaApiError.ServerUnavailable.left())
        verify(exactly = 0) { trackOrder(any(), any()) }
    }

    private suspend fun invokeUseCase() = useCase(
        userWalletId = USER_WALLET_ID,
        depositAddress = DEPOSIT_ADDRESS,
        signature = SIGNATURE,
    )

    private fun createOrderInfo(orderId: String = ORDER_ID) = TangemPayOrderInfo(
        orderId = orderId,
        orderStatus = OrderStatus.PROCESSING,
    )

    private fun createOrder(
        id: String = ORDER_ID,
        status: OrderStatus = OrderStatus.NEW,
        step: OrderStep = OrderStep.STARTING,
    ) = Order(
        id = id,
        customerId = null,
        type = OrderType.ACCOUNT_ISSUE_BANKING_RAIN,
        status = status,
        step = step,
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
        const val DEPOSIT_ADDRESS = "0xDEPOSIT"
        const val SIGNATURE = "0xSIG"
    }
}