package com.tangem.domain.pay.usecase

import arrow.core.left
import arrow.core.right
import com.google.common.truth.Truth.assertThat
import com.tangem.domain.models.wallet.UserWalletId
import com.tangem.domain.pay.model.Order
import com.tangem.domain.pay.model.OrderStatus
import com.tangem.domain.pay.model.OrderType
import com.tangem.domain.pay.repository.CustomerOrderRepository
import com.tangem.domain.visa.error.VisaApiError
import io.mockk.coEvery
import io.mockk.coVerify
import io.mockk.mockk
import kotlinx.coroutines.test.runTest
import org.junit.jupiter.api.Test

internal class RestoreActiveOrdersUseCaseTest {

    private val repository: CustomerOrderRepository = mockk()
    private val useCase = RestoreActiveOrdersUseCase(repository)
    private val userWalletId = UserWalletId("1234567890ABCDEF")

    @Test
    fun `passes only NEW and PROCESSING statuses to findOrders`() = runTest {
        val expected = setOf(OrderStatus.NEW, OrderStatus.PROCESSING)
        coEvery { repository.findOrders(userWalletId, types = emptySet(), statuses = expected) } returns
            emptyList<Order>().right()

        useCase(userWalletId)

        coVerify(exactly = 1) { repository.findOrders(userWalletId, types = emptySet(), statuses = expected) }
    }

    @Test
    fun `returns the orders found by the repository`() = runTest {
        val orders = listOf(
            order(id = "issue", type = OrderType.CARD_ISSUE_VIRTUAL_RAIN_KYC, status = OrderStatus.PROCESSING),
            order(id = "withdraw", type = OrderType.WITHDRAW, status = OrderStatus.NEW),
        )
        coEvery { repository.findOrders(userWalletId, types = emptySet(), statuses = any()) } returns orders.right()

        val result = useCase(userWalletId)

        assertThat(result.getOrNull()).containsExactlyElementsIn(orders)
    }

    @Test
    fun `surfaces repository errors`() = runTest {
        coEvery { repository.findOrders(userWalletId, types = emptySet(), statuses = any()) } returns
            VisaApiError.Unspecified.left()

        val result = useCase(userWalletId)

        assertThat(result.leftOrNull()).isEqualTo(VisaApiError.Unspecified)
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
}