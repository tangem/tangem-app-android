package com.tangem.domain.tangempay.order

import arrow.core.left
import arrow.core.right
import com.google.common.truth.Truth.assertThat
import com.tangem.domain.models.wallet.UserWalletId
import com.tangem.domain.tangempay.error.TangemPayApiError
import com.tangem.domain.tangempay.flow.PaymentAccountStatusFetcher
import com.tangem.domain.tangempay.model.OrderDetails
import com.tangem.domain.tangempay.model.OrderStep
import com.tangem.domain.tangempay.model.TangemPayOrder
import com.tangem.test.core.TestAppCoroutineScope
import io.mockk.Runs
import io.mockk.clearMocks
import io.mockk.coEvery
import io.mockk.coVerify
import io.mockk.just
import io.mockk.mockk
import kotlinx.coroutines.async
import kotlinx.coroutines.flow.toList
import kotlinx.coroutines.test.TestScope
import kotlinx.coroutines.test.runCurrent
import kotlinx.coroutines.test.runTest
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Test

internal class OrderPollerTest {

    private val orderRepository: TangemPayOrderRepository = mockk()
    private val ordersStore: OrdersStore = mockk()
    private val paymentAccountStatusFetcher: PaymentAccountStatusFetcher = mockk()
    private val activeOrdersRegistry = ActiveOrdersRegistry()

    @BeforeEach
    fun resetMocks() {
        clearMocks(orderRepository, ordersStore, paymentAccountStatusFetcher)
        coEvery { ordersStore.remove(any()) } just Runs
        coEvery { paymentAccountStatusFetcher(any<UserWalletId>()) } returns Unit.right()
    }

    @Test
    fun `GIVEN completed order WHEN poll THEN emits it as final and removes the stored order`() = runTest {
        // Arrange
        val completed = order(status = TangemPayOrder.Status.COMPLETED)
        coEvery { orderRepository.getOrder(USER_WALLET_ID, ORDER_ID) } returns completed.right()

        // Act
        val states = createPoller().poll(ORDER_REF).toList()

        // Assert
        assertThat(states).containsExactly(OrderPollingState.Pending, OrderPollingState.Loaded(completed)).inOrder()
        coVerify(exactly = 1) { ordersStore.remove(ORDER_REF) }
        coVerify(exactly = 1) { paymentAccountStatusFetcher(USER_WALLET_ID) }
    }

    @Test
    fun `GIVEN active then canceled order WHEN poll THEN emits both and ends on the canceled one`() = runTest {
        // Arrange
        val active = order(status = TangemPayOrder.Status.PROCESSING)
        val canceled = order(status = TangemPayOrder.Status.CANCELED)
        coEvery { orderRepository.getOrder(USER_WALLET_ID, ORDER_ID) } returnsMany listOf(
            active.right(),
            canceled.right(),
        )

        // Act
        val states = createPoller().poll(ORDER_REF).toList()

        // Assert
        assertThat(states).containsExactly(
            OrderPollingState.Pending,
            OrderPollingState.Loaded(active),
            OrderPollingState.Loaded(canceled),
        ).inOrder()
    }

    @Test
    fun `GIVEN step changes while active WHEN poll THEN account status refreshed on appearance, change and finish`() =
        runTest {
            // Arrange
            coEvery { orderRepository.getOrder(USER_WALLET_ID, ORDER_ID) } returnsMany listOf(
                order(status = TangemPayOrder.Status.PROCESSING, step = OrderStep.STARTING).right(),
                order(status = TangemPayOrder.Status.PROCESSING, step = OrderStep.INITIALIZATION).right(),
                order(status = TangemPayOrder.Status.COMPLETED).right(),
            )

            // Act
            createPoller().poll(ORDER_REF).toList()

            // Assert
            coVerify(exactly = 3) { paymentAccountStatusFetcher(USER_WALLET_ID) }
        }

    @Test
    fun `GIVEN step stays the same while active WHEN poll THEN account status refreshed on appearance and finish`() =
        runTest {
            // Arrange
            coEvery { orderRepository.getOrder(USER_WALLET_ID, ORDER_ID) } returnsMany listOf(
                order(status = TangemPayOrder.Status.PROCESSING, step = OrderStep.STARTING).right(),
                order(status = TangemPayOrder.Status.PROCESSING, step = OrderStep.STARTING).right(),
                order(status = TangemPayOrder.Status.COMPLETED).right(),
            )

            // Act
            createPoller().poll(ORDER_REF).toList()

            // Assert
            coVerify(exactly = 2) { paymentAccountStatusFetcher(USER_WALLET_ID) }
        }

    @Test
    fun `GIVEN unknown status WHEN poll THEN the order keeps being polled`() = runTest {
        // Arrange
        val unknown = order(status = TangemPayOrder.Status.UNKNOWN)
        val completed = order(status = TangemPayOrder.Status.COMPLETED)
        coEvery { orderRepository.getOrder(USER_WALLET_ID, ORDER_ID) } returnsMany listOf(
            unknown.right(),
            completed.right(),
        )

        // Act
        val states = createPoller().poll(ORDER_REF).toList()

        // Assert
        assertThat(states).containsExactly(
            OrderPollingState.Pending,
            OrderPollingState.Loaded(unknown),
            OrderPollingState.Loaded(completed),
        ).inOrder()
    }

    @Test
    fun `GIVEN request fails once WHEN poll THEN polling retries and finishes`() = runTest {
        // Arrange
        val completed = order(status = TangemPayOrder.Status.COMPLETED)
        coEvery { orderRepository.getOrder(USER_WALLET_ID, ORDER_ID) } returnsMany listOf(
            TangemPayApiError.Unspecified.left(),
            completed.right(),
        )

        // Act
        val states = createPoller().poll(ORDER_REF).toList()

        // Assert
        assertThat(states.last()).isEqualTo(OrderPollingState.Loaded(completed))
        coVerify(exactly = 2) { orderRepository.getOrder(USER_WALLET_ID, ORDER_ID) }
        coVerify(exactly = 1) { paymentAccountStatusFetcher(USER_WALLET_ID) }
    }

    @Test
    fun `GIVEN order not found WHEN poll THEN emits NotFound and removes the stored order`() = runTest {
        // Arrange
        coEvery {
            orderRepository.getOrder(USER_WALLET_ID, ORDER_ID)
        } returns TangemPayApiError.OrderNotFound.left()

        // Act
        val states = createPoller().poll(ORDER_REF).toList()

        // Assert
        assertThat(states.last()).isEqualTo(OrderPollingState.NotFound)
        coVerify(exactly = 1) { ordersStore.remove(ORDER_REF) }
        coVerify(exactly = 1) { paymentAccountStatusFetcher(USER_WALLET_ID) }
    }

    @Test
    fun `GIVEN order already polled WHEN poll again THEN subscribes to the existing polling`() = runTest {
        // Arrange
        val completed = order(status = TangemPayOrder.Status.COMPLETED)
        coEvery { orderRepository.getOrder(USER_WALLET_ID, ORDER_ID) } returnsMany listOf(
            order(status = TangemPayOrder.Status.PROCESSING).right(),
            completed.right(),
        )
        val poller = createPoller()

        // Act
        val first = async { poller.poll(ORDER_REF).toList() }
        val second = async { poller.poll(ORDER_REF).toList() }

        // Assert
        assertThat(first.await().last()).isEqualTo(OrderPollingState.Loaded(completed))
        assertThat(second.await().last()).isEqualTo(OrderPollingState.Loaded(completed))
        coVerify(exactly = 2) { orderRepository.getOrder(USER_WALLET_ID, ORDER_ID) }
    }

    @Test
    fun `GIVEN active order WHEN stopPolling for its wallet THEN emits Stopped and keeps the stored order`() = runTest {
        // Arrange
        coEvery {
            orderRepository.getOrder(USER_WALLET_ID, ORDER_ID)
        } returns order(status = TangemPayOrder.Status.PROCESSING).right()
        val poller = createPoller()
        val states = async { poller.poll(ORDER_REF).toList() }
        runCurrent()

        // Act
        poller.stopPolling(listOf(USER_WALLET_ID))

        // Assert
        assertThat(states.await().last()).isEqualTo(OrderPollingState.Stopped)
        coVerify(exactly = 0) { ordersStore.remove(any()) }
    }

    @Test
    fun `GIVEN active order WHEN stopPolling for another wallet THEN polling continues`() = runTest {
        // Arrange
        val completed = order(status = TangemPayOrder.Status.COMPLETED)
        coEvery { orderRepository.getOrder(USER_WALLET_ID, ORDER_ID) } returnsMany listOf(
            order(status = TangemPayOrder.Status.PROCESSING).right(),
            completed.right(),
        )
        val poller = createPoller()
        val states = async { poller.poll(ORDER_REF).toList() }
        runCurrent()

        // Act
        poller.stopPolling(listOf(UserWalletId("ddeeff")))

        // Assert
        assertThat(states.await().last()).isEqualTo(OrderPollingState.Loaded(completed))
    }

    @Test
    fun `GIVEN active order WHEN account status refreshed THEN the order is in the registry`() = runTest {
        // Arrange
        val active = order(status = TangemPayOrder.Status.PROCESSING)
        val registrySnapshots = mutableListOf<List<TangemPayOrder>>()
        coEvery { paymentAccountStatusFetcher(any<UserWalletId>()) } answers {
            registrySnapshots += activeOrdersRegistry.get(USER_WALLET_ID)
            Unit.right()
        }
        coEvery { orderRepository.getOrder(USER_WALLET_ID, ORDER_ID) } returnsMany listOf(
            active.right(),
            order(status = TangemPayOrder.Status.COMPLETED).right(),
        )

        // Act
        createPoller().poll(ORDER_REF).toList()

        // Assert
        assertThat(registrySnapshots).containsExactly(listOf(active), emptyList<TangemPayOrder>()).inOrder()
    }

    @Test
    fun `GIVEN active order WHEN stopPolling for its wallet THEN the order leaves the registry`() = runTest {
        // Arrange
        coEvery {
            orderRepository.getOrder(USER_WALLET_ID, ORDER_ID)
        } returns order(status = TangemPayOrder.Status.PROCESSING).right()
        val poller = createPoller()
        val states = async { poller.poll(ORDER_REF).toList() }
        runCurrent()

        // Act
        poller.stopPolling(listOf(USER_WALLET_ID))
        states.await()

        // Assert
        assertThat(activeOrdersRegistry.get(USER_WALLET_ID)).isEmpty()
    }

    private fun TestScope.createPoller() = OrderPoller(
        orderRepository = orderRepository,
        ordersStore = ordersStore,
        activeOrdersRegistry = activeOrdersRegistry,
        paymentAccountStatusFetcher = paymentAccountStatusFetcher,
        appCoroutineScope = TestAppCoroutineScope(backgroundScope.coroutineContext),
    )

    private fun order(status: TangemPayOrder.Status, step: OrderStep = OrderStep.UNKNOWN) = TangemPayOrder(
        id = ORDER_ID,
        customerId = "customer-1",
        status = status,
        step = step,
        stepChangeCode = 0,
        createdAt = "2026-11-09T10:00:00Z",
        updatedAt = "2026-11-09T10:00:00Z",
        errorCode = null,
        details = OrderDetails.CardFreeze(productInstanceId = "pi-1"),
    )

    private companion object {
        val USER_WALLET_ID = UserWalletId("aabbcc")
        const val ORDER_ID = "order-1"
        val ORDER_REF = OrderRef(userWalletId = USER_WALLET_ID, id = ORDER_ID)
    }
}