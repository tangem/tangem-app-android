package com.tangem.domain.pay.usecase

import arrow.core.left
import arrow.core.right
import com.google.common.truth.Truth.assertThat
import com.tangem.domain.models.wallet.UserWalletId
import com.tangem.domain.pay.flow.PaymentAccountStatusFetcher
import com.tangem.domain.pay.model.OrderStatus
import com.tangem.domain.pay.model.TangemPayOrderInfo
import com.tangem.domain.pay.model.TangemPayPendingOrder
import com.tangem.domain.pay.repository.TangemPayCloseCardRepository
import com.tangem.domain.pay.TangemPayOrderPollingScheduler
import com.tangem.domain.visa.error.VisaApiError
import io.mockk.coEvery
import io.mockk.coVerify
import io.mockk.coVerifyOrder
import io.mockk.mockk
import kotlinx.coroutines.test.runTest
import org.junit.jupiter.api.Test

internal class CloseTangemPayCardUseCaseTest {

    private val closeCardRepository: TangemPayCloseCardRepository = mockk(relaxUnitFun = true)
    private val pollingScheduler: TangemPayOrderPollingScheduler = mockk(relaxed = true)
    private val paymentAccountStatusFetcher: PaymentAccountStatusFetcher = mockk()

    private val useCase = CloseTangemPayCardUseCase(
        closeCardRepository = closeCardRepository,
        pollingScheduler = pollingScheduler,
        paymentAccountStatusFetcher = paymentAccountStatusFetcher,
    )

    @Test
    fun `GIVEN closeCard fails WHEN invoke THEN returns Left and skips schedule and fetch`() = runTest {
        coEvery { closeCardRepository.closeCard(USER_WALLET_ID, CARD_ID) } returns VisaApiError.Unspecified.left()

        val result = useCase(USER_WALLET_ID, CARD_ID)

        assertThat(result.isLeft()).isTrue()
        coVerify(exactly = 0) { pollingScheduler.scheduleOrderAsync(any()) }
        coVerify(exactly = 0) { paymentAccountStatusFetcher.invoke(any<UserWalletId>()) }
    }

    @Test
    fun `GIVEN closeCard returns CANCELED order WHEN invoke THEN returns Left and skips schedule and fetch`() = runTest {
        val order = TangemPayOrderInfo(ORDER_ID, OrderStatus.CANCELED)
        coEvery { closeCardRepository.closeCard(USER_WALLET_ID, CARD_ID) } returns order.right()

        val result = useCase(USER_WALLET_ID, CARD_ID)

        assertThat(result.isLeft()).isTrue()
        coVerify(exactly = 0) { pollingScheduler.scheduleOrderAsync(any()) }
        coVerify(exactly = 0) { paymentAccountStatusFetcher.invoke(any<UserWalletId>()) }
    }

    @Test
    fun `GIVEN closeCard returns PROCESSING order WHEN invoke THEN schedules CLOSE order then fetches status`() =
        runTest {
            val order = TangemPayOrderInfo(ORDER_ID, OrderStatus.PROCESSING)
            coEvery { closeCardRepository.closeCard(USER_WALLET_ID, CARD_ID) } returns order.right()
            coEvery { paymentAccountStatusFetcher.invoke(USER_WALLET_ID) } returns Unit.right()

            val result = useCase(USER_WALLET_ID, CARD_ID)

            assertThat(result.isRight()).isTrue()
            coVerifyOrder {
                pollingScheduler.scheduleOrderAsync(
                    TangemPayPendingOrder(
                        orderId = ORDER_ID,
                        userWalletId = USER_WALLET_ID,
                        cardId = CARD_ID,
                        type = TangemPayPendingOrder.Type.CLOSE,
                        status = OrderStatus.PROCESSING,
                    ),
                )
                paymentAccountStatusFetcher.invoke(USER_WALLET_ID)
            }
        }

    private companion object {
        val USER_WALLET_ID = UserWalletId("aabbcc112233")
        const val CARD_ID = "card-test-id"
        const val ORDER_ID = "order-test-1"
    }
}