package com.tangem.domain.pay.usecase

import arrow.core.left
import arrow.core.right
import com.google.common.truth.Truth.assertThat
import com.tangem.common.test.TestAppCoroutineScope
import com.tangem.domain.models.wallet.UserWalletId
import com.tangem.domain.pay.flow.PaymentAccountStatusFetcher
import com.tangem.domain.pay.model.OrderStatus
import com.tangem.domain.pay.model.TangemPayOrderInfo
import com.tangem.domain.pay.repository.TangemPayCloseCardRepository
import com.tangem.domain.visa.error.VisaApiError
import io.mockk.coEvery
import io.mockk.coVerify
import io.mockk.coVerifyOrder
import io.mockk.mockk
import kotlinx.coroutines.test.runTest
import org.junit.jupiter.api.Test

internal class CloseTangemPayCardUseCaseTest {

    private val closeCardRepository: TangemPayCloseCardRepository = mockk(relaxUnitFun = true)
    private val startPollingUseCase: StartTangemPayOrderPollingUseCase = mockk()
    private val paymentAccountStatusFetcher: PaymentAccountStatusFetcher = mockk()

    @Test
    fun `GIVEN closeCard fails WHEN invoke THEN returns Left and skips store, fetch and polling`() = runTest {
        val useCase = createUseCase()
        coEvery { closeCardRepository.closeCard(USER_WALLET_ID, CARD_ID) } returns VisaApiError.Unspecified.left()

        val result = useCase(USER_WALLET_ID, CARD_ID)

        assertThat(result.isLeft()).isTrue()
        coVerify(exactly = 0) { closeCardRepository.setCloseOrderId(any(), any()) }
        coVerify(exactly = 0) { paymentAccountStatusFetcher.invoke(any<UserWalletId>()) }
        coVerify(exactly = 0) { startPollingUseCase(any(), any()) }
    }

    @Test
    fun `GIVEN closeCard returns CANCELED order WHEN invoke THEN returns Left and skips store, fetch and polling`() =
        runTest {
            val useCase = createUseCase()
            val order = TangemPayOrderInfo(ORDER_ID, OrderStatus.CANCELED)
            coEvery { closeCardRepository.closeCard(USER_WALLET_ID, CARD_ID) } returns order.right()

            val result = useCase(USER_WALLET_ID, CARD_ID)

            assertThat(result.isLeft()).isTrue()
            coVerify(exactly = 0) { closeCardRepository.setCloseOrderId(any(), any()) }
            coVerify(exactly = 0) { paymentAccountStatusFetcher.invoke(any<UserWalletId>()) }
            coVerify(exactly = 0) { startPollingUseCase(any(), any()) }
        }

    @Test
    fun `GIVEN closeCard returns PROCESSING order WHEN invoke THEN stores order id, fetches status and starts polling`() =
        runTest {
            val useCase = createUseCase()
            val order = TangemPayOrderInfo(ORDER_ID, OrderStatus.PROCESSING)
            coEvery { closeCardRepository.closeCard(USER_WALLET_ID, CARD_ID) } returns order.right()
            coEvery { closeCardRepository.setCloseOrderId(CARD_ID, ORDER_ID) } returns Unit.right()
            coEvery { paymentAccountStatusFetcher.invoke(USER_WALLET_ID) } returns Unit.right()
            coEvery { startPollingUseCase(order, USER_WALLET_ID) } returns true

            val result = useCase(USER_WALLET_ID, CARD_ID)

            assertThat(result.isRight()).isTrue()
            coVerifyOrder {
                closeCardRepository.setCloseOrderId(CARD_ID, ORDER_ID)
                paymentAccountStatusFetcher.invoke(USER_WALLET_ID)
                startPollingUseCase(order, USER_WALLET_ID)
            }
        }

    @Test
    fun `GIVEN closeCard returns COMPLETED order WHEN invoke THEN stores order id, fetches status and starts polling`() =
        runTest {
            val useCase = createUseCase()
            val order = TangemPayOrderInfo(ORDER_ID, OrderStatus.COMPLETED)
            coEvery { closeCardRepository.closeCard(USER_WALLET_ID, CARD_ID) } returns order.right()
            coEvery { closeCardRepository.setCloseOrderId(CARD_ID, ORDER_ID) } returns Unit.right()
            coEvery { paymentAccountStatusFetcher.invoke(USER_WALLET_ID) } returns Unit.right()
            coEvery { startPollingUseCase(order, USER_WALLET_ID) } returns true

            val result = useCase(USER_WALLET_ID, CARD_ID)

            assertThat(result.isRight()).isTrue()
            coVerify(exactly = 1) { closeCardRepository.setCloseOrderId(CARD_ID, ORDER_ID) }
            coVerify(exactly = 1) { paymentAccountStatusFetcher.invoke(USER_WALLET_ID) }
            coVerify(exactly = 1) { startPollingUseCase(order, USER_WALLET_ID) }
        }

    private fun createUseCase() = CloseTangemPayCardUseCase(
        closeCardRepository = closeCardRepository,
        startTangemPayOrderPollingUseCase = startPollingUseCase,
        paymentAccountStatusFetcher = paymentAccountStatusFetcher,
        appCoroutineScope = TestAppCoroutineScope(),
    )

    private companion object {
        val USER_WALLET_ID = UserWalletId("aabbcc112233")
        const val CARD_ID = "card-test-id"
        const val ORDER_ID = "order-test-1"
    }
}