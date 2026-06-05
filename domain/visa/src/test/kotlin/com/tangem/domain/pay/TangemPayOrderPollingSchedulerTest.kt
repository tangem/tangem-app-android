package com.tangem.domain.pay

import arrow.core.right
import com.google.common.truth.Truth.assertThat
import com.tangem.domain.models.wallet.UserWalletId
import com.tangem.domain.pay.flow.PaymentAccountStatusFetcher
import com.tangem.domain.pay.model.OrderStatus
import com.tangem.domain.pay.model.TangemPayOrderInfo
import com.tangem.domain.pay.model.TangemPayPendingOrder
import com.tangem.domain.pay.repository.TangemPayPendingOrdersRepository
import com.tangem.domain.pay.usecase.StartTangemPayOrderPollingUseCase
import com.tangem.test.core.TestAppCoroutineScope
import io.mockk.coEvery
import io.mockk.coVerify
import io.mockk.mockk
import kotlinx.coroutines.delay
import kotlinx.coroutines.test.runTest
import org.junit.jupiter.api.Test

internal class TangemPayOrderPollingSchedulerTest {

    private val pendingOrdersRepository: TangemPayPendingOrdersRepository = mockk(relaxUnitFun = true)
    private val startTangemPayOrderPollingUseCase: StartTangemPayOrderPollingUseCase = mockk()
    private val paymentAccountStatusFetcher: PaymentAccountStatusFetcher = mockk()

    @Test
    fun `WHEN scheduleOrderAsync THEN saves order before polling`() = runTest {
        coEvery { startTangemPayOrderPollingUseCase(any(), any()) } returns true
        coEvery { paymentAccountStatusFetcher.invoke(any<UserWalletId>()) } returns Unit.right()

        createScheduler().scheduleOrderAsync(ORDER)

        coVerify(exactly = 1) { pendingOrdersRepository.save(ORDER) }
    }

    @Test
    fun `GIVEN polling succeeds WHEN scheduleOrderAsync THEN deferred resolves to true`() = runTest {
        coEvery { startTangemPayOrderPollingUseCase(any(), any()) } returns true
        coEvery { paymentAccountStatusFetcher.invoke(any<UserWalletId>()) } returns Unit.right()

        val deferred = createScheduler().scheduleOrderAsync(ORDER)

        assertThat(deferred.await()).isTrue()
        coVerify(exactly = 1) {
            startTangemPayOrderPollingUseCase(TangemPayOrderInfo(ORDER.orderId, ORDER.status), ORDER.userWalletId)
        }
    }

    @Test
    fun `GIVEN polling completes WHEN scheduleOrderAsync THEN removes order and fetches account status`() = runTest {
        coEvery { startTangemPayOrderPollingUseCase(any(), any()) } returns false
        coEvery { paymentAccountStatusFetcher.invoke(any<UserWalletId>()) } returns Unit.right()

        createScheduler().scheduleOrderAsync(ORDER)

        coVerify(exactly = 1) { pendingOrdersRepository.remove(ORDER.orderId) }
        coVerify(exactly = 1) { paymentAccountStatusFetcher.invoke(ORDER.userWalletId) }
    }

    @Test
    fun `GIVEN stored orders WHEN resumeAll THEN starts polling for every order`() = runTest {
        val secondOrder = ORDER.copy(orderId = "order-2", userWalletId = UserWalletId("ddeeff445566"))
        coEvery { pendingOrdersRepository.getAll() } returns listOf(ORDER, secondOrder)
        coEvery { startTangemPayOrderPollingUseCase(any(), any()) } returns true
        coEvery { paymentAccountStatusFetcher.invoke(any<UserWalletId>()) } returns Unit.right()

        createScheduler().resumeAll()

        coVerify(exactly = 1) {
            startTangemPayOrderPollingUseCase(
                TangemPayOrderInfo(ORDER.orderId, ORDER.status),
                ORDER.userWalletId
            )
        }
        coVerify(exactly = 1) {
            startTangemPayOrderPollingUseCase(
                TangemPayOrderInfo(secondOrder.orderId, secondOrder.status),
                secondOrder.userWalletId,
            )
        }
    }

    private fun createScheduler() = TangemPayOrderPollingScheduler(
        pendingOrdersRepository = pendingOrdersRepository,
        startTangemPayOrderPollingUseCase = startTangemPayOrderPollingUseCase,
        paymentAccountStatusFetcher = paymentAccountStatusFetcher,
        appCoroutineScope = TestAppCoroutineScope(),
    )

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