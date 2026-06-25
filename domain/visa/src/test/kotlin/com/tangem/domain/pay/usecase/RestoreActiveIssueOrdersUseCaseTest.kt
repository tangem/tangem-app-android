package com.tangem.domain.pay.usecase

import arrow.core.left
import arrow.core.right
import com.google.common.truth.Truth.assertThat
import com.tangem.domain.models.wallet.UserWalletId
import com.tangem.domain.pay.model.Order
import com.tangem.domain.pay.model.OrderStatus
import com.tangem.domain.pay.model.OrderType
import com.tangem.domain.pay.model.TangemPayOrderInfo
import com.tangem.domain.pay.repository.CustomerOrderRepository
import com.tangem.domain.pay.repository.TangemPayIssueCardRepository
import com.tangem.domain.visa.error.VisaApiError
import com.tangem.test.core.TestAppCoroutineScope
import io.mockk.coEvery
import io.mockk.coVerify
import io.mockk.mockk
import kotlinx.coroutines.test.runTest
import org.junit.jupiter.api.Test

internal class RestoreActiveIssueOrdersUseCaseTest {

    private val orderRepository: CustomerOrderRepository = mockk()
    private val issueCardRepository: TangemPayIssueCardRepository = mockk(relaxed = true)
    private val startTangemPayOrderPollingUseCase: StartTangemPayOrderPollingUseCase = mockk(relaxed = true)
    private val useCase = RestoreActiveIssueOrdersUseCase(
        customerOrderRepository = orderRepository,
        issueCardRepository = issueCardRepository,
        startTangemPayOrderPollingUseCase = startTangemPayOrderPollingUseCase,
        appCoroutineScope = TestAppCoroutineScope(),
    )
    private val userWalletId = UserWalletId("1234567890ABCDEF")

    @Test
    fun `GIVEN active issue orders WHEN invoke THEN each order is stored and polled`() = runTest {
        // Arrange
        val first = order(id = "first", type = OrderType.CARD_ISSUE_VIRTUAL_RAIN, status = OrderStatus.NEW)
        val second = order(id = "second", type = OrderType.CARD_ISSUE_VIRTUAL_RAIN_KYC, status = OrderStatus.PROCESSING)
        coEvery {
            orderRepository.findOrders(
                userWalletId = userWalletId,
                types = ISSUE_ORDER_TYPES,
                statuses = ACTIVE_STATUSES,
            )
        } returns listOf(first, second).right()

        // Act
        val result = useCase(userWalletId)

        // Assert
        assertThat(result.isRight()).isTrue()
        coVerify(exactly = 1) { issueCardRepository.storeIssueOrderId(userWalletId, first.id) }
        coVerify(exactly = 1) { issueCardRepository.storeIssueOrderId(userWalletId, second.id) }
        coVerify(exactly = 1) {
            startTangemPayOrderPollingUseCase(TangemPayOrderInfo(first.id, first.status), userWalletId)
        }
        coVerify(exactly = 1) {
            startTangemPayOrderPollingUseCase(TangemPayOrderInfo(second.id, second.status), userWalletId)
        }
    }

    @Test
    fun `GIVEN no active orders WHEN invoke THEN nothing is stored or polled`() = runTest {
        // Arrange
        coEvery {
            orderRepository.findOrders(userWalletId, types = ISSUE_ORDER_TYPES, statuses = ACTIVE_STATUSES)
        } returns emptyList<Order>().right()

        // Act
        val result = useCase(userWalletId)

        // Assert
        assertThat(result.isRight()).isTrue()
        coVerify(exactly = 0) { issueCardRepository.storeIssueOrderId(any(), any()) }
        coVerify(exactly = 0) { startTangemPayOrderPollingUseCase(any(), any()) }
    }

    @Test
    fun `GIVEN a terminal order leaks through WHEN invoke THEN it is filtered out`() = runTest {
        // Arrange
        val completed = order(id = "done", type = OrderType.CARD_ISSUE_VIRTUAL_RAIN, status = OrderStatus.COMPLETED)
        coEvery {
            orderRepository.findOrders(userWalletId, types = ISSUE_ORDER_TYPES, statuses = ACTIVE_STATUSES)
        } returns listOf(completed).right()

        // Act
        val result = useCase(userWalletId)

        // Assert
        assertThat(result.isRight()).isTrue()
        coVerify(exactly = 0) { issueCardRepository.storeIssueOrderId(any(), any()) }
        coVerify(exactly = 0) { startTangemPayOrderPollingUseCase(any(), any()) }
    }

    @Test
    fun `GIVEN findOrders fails WHEN invoke THEN returns Unspecified and stores nothing`() = runTest {
        // Arrange
        coEvery {
            orderRepository.findOrders(userWalletId, types = ISSUE_ORDER_TYPES, statuses = ACTIVE_STATUSES)
        } returns VisaApiError.Unspecified.left()

        // Act
        val result = useCase(userWalletId)

        // Assert
        assertThat(result.leftOrNull()).isEqualTo(VisaApiError.Unspecified)
        coVerify(exactly = 0) { issueCardRepository.storeIssueOrderId(any(), any()) }
        coVerify(exactly = 0) { startTangemPayOrderPollingUseCase(any(), any()) }
    }

    private fun order(id: String, type: OrderType, status: OrderStatus): Order = Order(
        id = id,
        customerId = "customer",
        type = type,
        status = status,
        step = null,
        stepChangeCode = null,
        productInstanceId = null,
        paymentAccountId = null,
        cardId = null,
        withdrawTxHash = null,
        createdAt = null,
        updatedAt = null,
    )

    private companion object {
        val ISSUE_ORDER_TYPES = OrderType.issueCardTypes
        val ACTIVE_STATUSES = OrderStatus.activeStatuses
    }
}