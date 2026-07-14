package com.tangem.domain.pay.usecase

import arrow.core.left
import arrow.core.right
import com.google.common.truth.Truth.assertThat
import com.tangem.domain.models.wallet.UserWalletId
import com.tangem.domain.pay.flow.PaymentAccountStatusFetcher
import com.tangem.domain.pay.model.OrderStatus
import com.tangem.domain.pay.model.TangemPayOrderInfo
import com.tangem.domain.pay.repository.CustomerOrderRepository
import com.tangem.domain.visa.error.VisaApiError
import io.mockk.coEvery
import io.mockk.coVerify
import io.mockk.mockk
import kotlinx.coroutines.test.runTest
import org.junit.jupiter.api.Test

internal class CancelTangemPayOrderUseCaseTest {

    private val customerOrderRepository: CustomerOrderRepository = mockk()
    private val paymentAccountStatusFetcher: PaymentAccountStatusFetcher = mockk()
    private val startTangemPayOrderPollingUseCase: StartTangemPayOrderPollingUseCase = mockk()

    private val useCase = CancelTangemPayOrderUseCase(
        customerOrderRepository = customerOrderRepository,
        paymentAccountStatusFetcher = paymentAccountStatusFetcher,
        startTangemPayOrderPollingUseCase = startTangemPayOrderPollingUseCase,
    )

    @Test
    fun `GIVEN cancelOrder fails WHEN invoke THEN returns Left and skips fetch and polling`() = runTest {
        // GIVEN
        coEvery { customerOrderRepository.cancelOrder(USER_WALLET_ID, ORDER_ID) } returns VisaApiError.Unspecified.left()

        // WHEN
        val result = useCase(USER_WALLET_ID, ORDER_ID)

        // THEN
        assertThat(result.leftOrNull()).isEqualTo(VisaApiError.Unspecified)
        coVerify(exactly = 0) { paymentAccountStatusFetcher.invoke(any<UserWalletId>()) }
        coVerify(exactly = 0) { startTangemPayOrderPollingUseCase(any(), any(), any()) }
    }

    @Test
    fun `GIVEN cancelOrder succeeds WHEN invoke THEN fetches status and starts polling with PROCESSING order`() =
        runTest {
            // GIVEN
            coEvery { customerOrderRepository.cancelOrder(USER_WALLET_ID, ORDER_ID) } returns Unit.right()
            coEvery { paymentAccountStatusFetcher.invoke(USER_WALLET_ID) } returns Unit.right()
            coEvery { startTangemPayOrderPollingUseCase(any(), any(), any()) } returns true

            // WHEN
            val result = useCase(USER_WALLET_ID, ORDER_ID)

            // THEN
            assertThat(result.isRight()).isTrue()
            coVerify(exactly = 1) { paymentAccountStatusFetcher.invoke(USER_WALLET_ID) }
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