package com.tangem.domain.tangempay.usecase.order

import arrow.core.left
import arrow.core.right
import com.google.common.truth.Truth.assertThat
import com.tangem.domain.models.wallet.UserWalletId
import com.tangem.domain.tangempay.error.TangemPayApiError
import com.tangem.domain.tangempay.model.OrderDetails
import com.tangem.domain.tangempay.model.OrderStep
import com.tangem.domain.tangempay.model.TangemPayOrder
import com.tangem.domain.tangempay.order.OrderPoller
import com.tangem.domain.tangempay.order.OrdersStore
import com.tangem.domain.tangempay.order.OrderRef
import com.tangem.domain.tangempay.order.TangemPayOrderRepository
import io.mockk.Runs
import io.mockk.clearMocks
import io.mockk.coEvery
import io.mockk.coVerify
import io.mockk.coVerifyOrder
import io.mockk.every
import io.mockk.just
import io.mockk.mockk
import io.mockk.verify
import kotlinx.coroutines.flow.emptyFlow
import kotlinx.coroutines.test.currentTime
import kotlinx.coroutines.test.runTest
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Test

internal class RestoreOrdersUseCaseTest {

    private val orderRepository: TangemPayOrderRepository = mockk()
    private val ordersStore: OrdersStore = mockk()
    private val orderPoller: OrderPoller = mockk()

    private val useCase = RestoreOrdersUseCase(
        orderRepository = orderRepository,
        ordersStore = ordersStore,
        orderPoller = orderPoller,
    )

    @BeforeEach
    fun resetMocks() {
        clearMocks(orderRepository, ordersStore, orderPoller)
        coEvery { ordersStore.getAll(USER_WALLET_ID) } returns emptyList()
        coEvery { ordersStore.save(any()) } just Runs
        every { orderPoller.poll(any()) } returns emptyFlow()
        coEvery { orderRepository.findOrders(any(), any(), any()) } returns emptyList<TangemPayOrder>().right()
    }

    @Test
    fun `GIVEN stored orders WHEN invoke THEN each stored order is polled`() = runTest {
        // Arrange
        coEvery { ordersStore.getAll(USER_WALLET_ID) } returns listOf(STORED_ORDER)

        // Act
        useCase(USER_WALLET_ID)

        // Assert
        verify(exactly = 1) { orderPoller.poll(STORED_ORDER) }
        coVerify(exactly = 0) { ordersStore.save(any()) }
    }

    @Test
    fun `GIVEN active backend order not stored WHEN invoke THEN it is saved and polled`() = runTest {
        // Arrange
        val backendOrder = OrderRef(userWalletId = USER_WALLET_ID, id = "backend-order")
        coEvery { orderRepository.findOrders(any(), any(), any()) } returns listOf(order(backendOrder.id)).right()

        // Act
        useCase(USER_WALLET_ID)

        // Assert
        coVerifyOrder {
            ordersStore.save(backendOrder)
            orderPoller.poll(backendOrder)
        }
    }

    @Test
    fun `GIVEN active backend order already stored WHEN invoke THEN it is polled once and not saved again`() =
        runTest {
            // Arrange
            coEvery { ordersStore.getAll(USER_WALLET_ID) } returns listOf(STORED_ORDER)
            coEvery { orderRepository.findOrders(any(), any(), any()) } returns listOf(order(STORED_ORDER.id)).right()

            // Act
            useCase(USER_WALLET_ID)

            // Assert
            verify(exactly = 1) { orderPoller.poll(STORED_ORDER) }
            coVerify(exactly = 0) { ordersStore.save(any()) }
        }

    @Test
    fun `GIVEN wallet WHEN invoke THEN active freeze and unfreeze orders are requested`() = runTest {
        // Act
        useCase(USER_WALLET_ID)

        // Assert
        coVerify(exactly = 1) {
            orderRepository.findOrders(
                userWalletId = USER_WALLET_ID,
                types = setOf(TangemPayOrder.Type.CARD_FREEZE, TangemPayOrder.Type.CARD_UNFREEZE),
                statuses = setOf(TangemPayOrder.Status.NEW, TangemPayOrder.Status.PROCESSING),
            )
        }
    }

    @Test
    fun `GIVEN backend fails twice WHEN invoke THEN stored orders are polled at once and backend is retried`() =
        runTest {
            // Arrange
            val backendOrder = OrderRef(userWalletId = USER_WALLET_ID, id = "backend-order")
            coEvery { ordersStore.getAll(USER_WALLET_ID) } returns listOf(STORED_ORDER)
            coEvery { orderRepository.findOrders(any(), any(), any()) } returnsMany listOf(
                TangemPayApiError.Unspecified.left(),
                TangemPayApiError.Unspecified.left(),
                listOf(order(backendOrder.id)).right(),
            )

            // Act
            useCase(USER_WALLET_ID)

            // Assert
            coVerifyOrder {
                orderPoller.poll(STORED_ORDER)
                orderRepository.findOrders(any(), any(), any())
                orderRepository.findOrders(any(), any(), any())
                orderRepository.findOrders(any(), any(), any())
                ordersStore.save(backendOrder)
                orderPoller.poll(backendOrder)
            }
            assertThat(currentTime).isEqualTo(RETRY_DELAY_MILLIS * 2)
        }

    private fun order(id: String) = TangemPayOrder(
        id = id,
        status = TangemPayOrder.Status.PROCESSING,
        step = OrderStep.UNKNOWN,
        details = OrderDetails.CardFreeze(productInstanceId = "pi-1"),
    )

    private companion object {
        val USER_WALLET_ID = UserWalletId("aabbcc")
        val STORED_ORDER = OrderRef(userWalletId = USER_WALLET_ID, id = "stored-order")
        const val RETRY_DELAY_MILLIS = 5_000L
    }
}