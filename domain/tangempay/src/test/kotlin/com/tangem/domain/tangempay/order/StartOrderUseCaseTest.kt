package com.tangem.domain.tangempay.order

import arrow.core.left
import arrow.core.right
import com.google.common.truth.Truth.assertThat
import com.tangem.domain.models.wallet.UserWalletId
import com.tangem.domain.tangempay.error.TangemPayApiError
import com.tangem.domain.tangempay.model.CustomerOrderRequest
import com.tangem.domain.tangempay.model.OrderDetails
import com.tangem.domain.tangempay.model.OrderStep
import com.tangem.domain.tangempay.model.OrderType
import com.tangem.domain.tangempay.model.TangemPayOrder
import com.tangem.domain.tangempay.usecase.order.StartOrderUseCase
import io.mockk.Runs
import io.mockk.clearMocks
import io.mockk.coEvery
import io.mockk.coVerify
import io.mockk.coVerifyOrder
import io.mockk.every
import io.mockk.just
import io.mockk.mockk
import io.mockk.verify
import kotlinx.coroutines.flow.flowOf
import kotlinx.coroutines.test.runTest
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Test

internal class StartOrderUseCaseTest {

    private val orderRepository: TangemPayOrderRepository = mockk()
    private val ordersStore: OrdersStore = mockk()
    private val orderPoller: OrderPoller = mockk()

    private val useCase = StartOrderUseCase(
        orderRepository = orderRepository,
        ordersStore = ordersStore,
        orderPoller = orderPoller,
    )

    @BeforeEach
    fun resetMocks() {
        clearMocks(orderRepository, ordersStore, orderPoller)
        coEvery { ordersStore.save(any()) } just Runs
        every { orderPoller.poll(any()) } returns flowOf(OrderPollingState.Pending)
    }

    @Test
    fun `GIVEN data launch WHEN invoke THEN creates, saves and polls the order`() = runTest {
        // Arrange
        coEvery {
            orderRepository.createOrder(USER_WALLET_ID, CREATE_REQUEST, IDEMPOTENCY_KEY)
        } returns createdOrder().right()
        val expected = OrderRef(userWalletId = USER_WALLET_ID, id = ORDER_ID)

        // Act
        val result = useCase(
            userWalletId = USER_WALLET_ID,
            launch = OrderLaunch.Data(
                request = CREATE_REQUEST,
                idempotencyKey = IDEMPOTENCY_KEY,
            ),
        )

        // Assert
        assertThat(result.getOrNull()?.ref).isEqualTo(expected)
        coVerifyOrder {
            ordersStore.save(expected)
            orderPoller.poll(expected)
        }
    }

    @Test
    fun `GIVEN create fails WHEN invoke THEN returns the error without saving or polling`() = runTest {
        // Arrange
        coEvery {
            orderRepository.createOrder(USER_WALLET_ID, CREATE_REQUEST, IDEMPOTENCY_KEY)
        } returns TangemPayApiError.Unspecified.left()

        // Act
        val result = useCase(
            userWalletId = USER_WALLET_ID,
            launch = OrderLaunch.Data(
                request = CREATE_REQUEST,
                idempotencyKey = IDEMPOTENCY_KEY,
            ),
        )

        // Assert
        assertThat(result.leftOrNull()).isEqualTo(TangemPayApiError.Unspecified)
        coVerify(exactly = 0) { ordersStore.save(any()) }
        verify(exactly = 0) { orderPoller.poll(any()) }
    }

    @Test
    fun `GIVEN id launch WHEN invoke THEN saves and polls without creating`() = runTest {
        // Arrange
        val expected = OrderRef(userWalletId = USER_WALLET_ID, id = ORDER_ID)

        // Act
        val result = useCase(
            userWalletId = USER_WALLET_ID,
            launch = OrderLaunch.Id(orderId = ORDER_ID),
        )

        // Assert
        assertThat(result.getOrNull()?.ref).isEqualTo(expected)
        coVerify(exactly = 0) { orderRepository.createOrder(any(), any(), any()) }
        coVerifyOrder {
            ordersStore.save(expected)
            orderPoller.poll(expected)
        }
    }

    private fun createdOrder() = TangemPayOrder(
        id = ORDER_ID,
        status = TangemPayOrder.Status.NEW,
        step = OrderStep.UNKNOWN,
        details = OrderDetails.Unknown,
    )

    private companion object {
        val USER_WALLET_ID = UserWalletId("aabbcc")
        const val ORDER_ID = "order-1"
        const val IDEMPOTENCY_KEY = "key-1"
        val CREATE_REQUEST: CustomerOrderRequest = CustomerOrderRequest.CardIssue(
            type = OrderType.CARD_ISSUE_VIRTUAL_RAIN,
            specificationName = "SP_1",
        )
    }
}