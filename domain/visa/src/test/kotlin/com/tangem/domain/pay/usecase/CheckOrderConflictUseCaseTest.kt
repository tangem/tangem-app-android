package com.tangem.domain.pay.usecase

import arrow.core.left
import arrow.core.right
import com.google.common.truth.Truth.assertThat
import com.tangem.domain.models.wallet.UserWalletId
import com.tangem.domain.pay.model.*
import com.tangem.domain.pay.repository.CustomerOrderRepository
import com.tangem.domain.visa.error.VisaApiError
import io.mockk.coEvery
import io.mockk.mockk
import kotlinx.coroutines.test.runTest
import org.junit.jupiter.api.Test

internal class CheckOrderConflictUseCaseTest {

    private val repository: CustomerOrderRepository = mockk()
    private val useCase = CheckOrderConflictUseCase(repository)
    private val userWalletId = UserWalletId("1234567890ABCDEF")

    @Test
    fun `WHEN no active orders THEN returns Allowed`() = runTest {
        coEvery { repository.findOrders(userWalletId, types = emptySet(), statuses = ACTIVE_STATUSES) } returns
            emptyList<Order>().right()

        val result = useCase(userWalletId, OrderIntent.IssueCard)

        assertThat(result.getOrNull()).isEqualTo(ConflictResolution.Allowed)
    }

    @Test
    fun `WHEN active issue order exists AND intent is IssueCard THEN returns Blocked`() = runTest {
        val activeIssue = order(type = OrderType.CARD_ISSUE_ADDITIONAL, status = OrderStatus.PROCESSING)
        coEvery { repository.findOrders(userWalletId, types = emptySet(), statuses = ACTIVE_STATUSES) } returns
            listOf(activeIssue).right()

        val result = useCase(userWalletId, OrderIntent.IssueCard)

        val resolution = result.getOrNull()
        assertThat(resolution).isInstanceOf(ConflictResolution.Blocked::class.java)
        assertThat((resolution as ConflictResolution.Blocked).blockingOrder).isEqualTo(activeIssue)
    }

    @Test
    fun `WHEN repository fails THEN returns Either Left`() = runTest {
        coEvery { repository.findOrders(userWalletId, types = emptySet(), statuses = ACTIVE_STATUSES) } returns
            VisaApiError.Unspecified.left()

        val result = useCase(userWalletId, OrderIntent.IssueCard)

        assertThat(result.leftOrNull()).isEqualTo(VisaApiError.Unspecified)
    }

    private fun order(type: OrderType, status: OrderStatus): Order = Order(
        id = "id",
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
        val ACTIVE_STATUSES: Set<OrderStatus> = setOf(OrderStatus.NEW, OrderStatus.PROCESSING)
    }
}