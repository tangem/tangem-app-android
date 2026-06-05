package com.tangem.domain.pay.usecase

import com.google.common.truth.Truth.assertThat
import com.tangem.domain.models.wallet.UserWalletId
import com.tangem.domain.pay.model.OrderStatus
import com.tangem.domain.pay.model.TangemPayPendingOrder
import com.tangem.domain.pay.repository.TangemPayPendingOrdersRepository
import io.mockk.every
import io.mockk.mockk
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.flowOf
import kotlinx.coroutines.test.runTest
import org.junit.jupiter.api.Test

internal class GetTangemPayPendingOrdersUseCaseTest {

    private val pendingOrdersRepository: TangemPayPendingOrdersRepository = mockk()

    private val useCase = GetTangemPayPendingOrdersUseCase(
        pendingOrdersRepository = pendingOrdersRepository,
    )

    @Test
    fun `GIVEN repository emits orders WHEN invoke THEN emits the same orders`() = runTest {
        val orders = listOf(ORDER)
        every { pendingOrdersRepository.getAllFlow() } returns flowOf(orders)

        val result = useCase().first()

        assertThat(result).isEqualTo(orders)
    }

    private companion object {
        val ORDER = TangemPayPendingOrder(
            orderId = "order-1",
            userWalletId = UserWalletId("aabbcc112233"),
            cardId = "card-1",
            type = TangemPayPendingOrder.Type.REISSUE,
            status = OrderStatus.NEW,
        )
    }
}