package com.tangem.domain.moneyaccount.usecase

import com.tangem.domain.models.wallet.UserWalletId
import com.tangem.domain.moneyaccount.flow.MoneyAccountStatusFetcher
import com.tangem.domain.moneyaccount.repository.MoneyAccountOrderRepository
import com.tangem.domain.pay.model.OrderStatus
import com.tangem.domain.pay.model.OrderStep
import com.tangem.domain.pay.model.TangemPayOrderInfo
import com.tangem.domain.pay.usecase.StartTangemPayOrderPollingUseCase
import com.tangem.test.core.TestAppCoroutineScope
import io.mockk.clearMocks
import io.mockk.coEvery
import io.mockk.coVerify
import io.mockk.every
import io.mockk.just
import io.mockk.mockk
import io.mockk.runs
import io.mockk.verify
import kotlinx.coroutines.test.advanceUntilIdle
import kotlinx.coroutines.test.runTest
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.TestInstance

@TestInstance(TestInstance.Lifecycle.PER_CLASS)
internal class TrackMoneyAccountOrderUseCaseTest {

    private val orderRepository: MoneyAccountOrderRepository = mockk()
    private val startPolling: StartTangemPayOrderPollingUseCase = mockk()
    private val moneyAccountStatusFetcher: MoneyAccountStatusFetcher = mockk(relaxed = true)

    private var onOrderStateChange: (suspend (TangemPayOrderInfo) -> Unit)? = null
    private var onTerminalRefresh: (suspend (UserWalletId) -> Unit)? = null

    @BeforeEach
    fun resetMocks() {
        clearMocks(orderRepository, startPolling, moneyAccountStatusFetcher)
        onOrderStateChange = null
        onTerminalRefresh = null
        every { orderRepository.storeOrder(any(), any()) } just runs
        every { orderRepository.clearOrder(any()) } just runs
        coEvery { startPolling(any(), any(), any(), any(), any()) } coAnswers {
            onOrderStateChange = arg(2)
            onTerminalRefresh = arg(3)
            true
        }
    }

    @Test
    fun `GIVEN an order WHEN invoke THEN it is stored and polling starts`() = runTest {
        // Arrange
        val order = createOrder()

        // Act
        createUseCase().invoke(USER_WALLET_ID, order)
        advanceUntilIdle()

        // Assert
        verify(exactly = 1) { orderRepository.storeOrder(USER_WALLET_ID, order) }
        coVerify(exactly = 1) { startPolling(order, USER_WALLET_ID, any(), any(), any()) }
    }

    @Test
    fun `GIVEN a non-terminal update WHEN polling reports it THEN status is refreshed and order is kept`() =
        runTest {
            // Arrange
            createUseCase().invoke(USER_WALLET_ID, createOrder())
            advanceUntilIdle()
            val updated = createOrder(status = OrderStatus.PROCESSING, step = OrderStep.AWAITING_KYC)

            // Act
            requireNotNull(onOrderStateChange).invoke(updated)

            // Assert
            verify(exactly = 1) { orderRepository.storeOrder(USER_WALLET_ID, updated) }
            coVerify(exactly = 1) { moneyAccountStatusFetcher.invoke(USER_WALLET_ID) }
            verify(exactly = 0) { orderRepository.clearOrder(any()) }
        }

    @Test
    fun `GIVEN a terminal update WHEN polling reports it THEN order is cleared`() = runTest {
        // Arrange
        createUseCase().invoke(USER_WALLET_ID, createOrder())
        advanceUntilIdle()
        val completed = createOrder(status = OrderStatus.COMPLETED, step = OrderStep.ACCOUNT_ACTIVATION)

        // Act
        requireNotNull(onOrderStateChange).invoke(completed)

        // Assert
        verify(exactly = 1) { orderRepository.clearOrder(USER_WALLET_ID) }
    }

    @Test
    fun `GIVEN polling finished WHEN terminal refresh requested THEN account status is fetched`() = runTest {
        // Arrange
        createUseCase().invoke(USER_WALLET_ID, createOrder())
        advanceUntilIdle()

        // Act
        requireNotNull(onTerminalRefresh).invoke(USER_WALLET_ID)

        // Assert
        coVerify(exactly = 1) { moneyAccountStatusFetcher.invoke(USER_WALLET_ID) }
    }

    private fun kotlinx.coroutines.test.TestScope.createUseCase() = TrackMoneyAccountOrderUseCase(
        orderRepository = orderRepository,
        startPolling = startPolling,
        moneyAccountStatusFetcher = moneyAccountStatusFetcher,
        appCoroutineScope = TestAppCoroutineScope(testScope = this),
    )

    private fun createOrder(
        status: OrderStatus = OrderStatus.NEW,
        step: OrderStep = OrderStep.STARTING,
    ) = TangemPayOrderInfo(
        orderId = ORDER_ID,
        orderStatus = status,
        orderStep = step,
    )

    private companion object {
        val USER_WALLET_ID = UserWalletId("011")
        const val ORDER_ID = "order-1"
    }
}