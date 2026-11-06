package com.tangem.domain.tangempay.usecase

import arrow.core.left
import arrow.core.right
import com.google.common.truth.Truth.assertThat
import com.tangem.domain.models.wallet.UserWalletId
import com.tangem.domain.tangempay.model.OrderStatus
import com.tangem.domain.tangempay.model.TangemPayOrderInfo
import com.tangem.domain.tangempay.repository.CustomerOrderRepository
import com.tangem.domain.tangempay.error.TangemPayApiError
import io.mockk.coEvery
import io.mockk.coVerify
import io.mockk.mockk
import kotlinx.coroutines.test.runTest
import org.junit.jupiter.api.Test

internal class CancelTangemPayOrderUseCaseTest {

    private val customerOrderRepository: CustomerOrderRepository = mockk()
    private val startTangemPayOrderPollingUseCase: StartTangemPayOrderPollingUseCase = mockk()

    private val useCase = CancelTangemPayOrderUseCase(
        customerOrderRepository = customerOrderRepository,
        startTangemPayOrderPollingUseCase = startTangemPayOrderPollingUseCase,
    )

    @Test
    fun `GIVEN cancelOrder fails WHEN invoke THEN returns Left and skips polling`() = runTest {
        // GIVEN
        coEvery { customerOrderRepository.cancelOrder(USER_WALLET_ID, ORDER_ID) } returns TangemPayApiError.Unspecified.left()

        // WHEN
        val result = useCase(USER_WALLET_ID, ORDER_ID)

        // THEN
        assertThat(result.leftOrNull()).isEqualTo(TangemPayApiError.Unspecified)
        coVerify(exactly = 0) { startTangemPayOrderPollingUseCase(any(), any(), any()) }
    }

    @Test
    fun `GIVEN cancelOrder succeeds WHEN invoke THEN starts polling with PROCESSING order`() = runTest {
        // GIVEN
        coEvery { customerOrderRepository.cancelOrder(USER_WALLET_ID, ORDER_ID) } returns Unit.right()
        coEvery { startTangemPayOrderPollingUseCase(any(), any(), any()) } returns true

        // WHEN
        val result = useCase(USER_WALLET_ID, ORDER_ID)

        // THEN
        assertThat(result.isRight()).isTrue()
        coVerify(exactly = 1) {
            startTangemPayOrderPollingUseCase(
                TangemPayOrderInfo(orderId = ORDER_ID, orderStatus = OrderStatus.PROCESSING),
                USER_WALLET_ID,
                any(),
            )
        }
    }

    private companion object {
        val USER_WALLET_ID = UserWalletId("aabbcc112233")
        const val ORDER_ID = "order-test-1"
    }
}