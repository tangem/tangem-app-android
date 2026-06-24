package com.tangem.domain.pay.usecase

import arrow.core.left
import arrow.core.right
import com.google.common.truth.Truth.assertThat
import com.tangem.domain.models.wallet.UserWalletId
import com.tangem.domain.pay.flow.PaymentAccountStatusFetcher
import com.tangem.domain.pay.model.OrderStatus
import com.tangem.domain.pay.model.TangemPayOrderInfo
import com.tangem.domain.pay.repository.TangemPayCardDetailsRepository
import com.tangem.domain.visa.error.VisaApiError
import io.mockk.coEvery
import io.mockk.coVerify
import io.mockk.mockk
import kotlinx.coroutines.launch
import kotlinx.coroutines.test.runCurrent
import kotlinx.coroutines.test.runTest
import org.junit.jupiter.api.Test

internal class StartTangemPayOrderPollingUseCaseTest {

    private val cardDetailsRepository: TangemPayCardDetailsRepository = mockk()
    private val paymentAccountStatusFetcher: PaymentAccountStatusFetcher = mockk()

    private val useCase = StartTangemPayOrderPollingUseCase(
        cardDetailsRepository = cardDetailsRepository,
        paymentAccountStatusFetcher = paymentAccountStatusFetcher,
    )

    @Test
    fun `GIVEN order already COMPLETED WHEN invoke THEN returns true without polling`() = runTest {
        val order = TangemPayOrderInfo(ORDER_ID, OrderStatus.COMPLETED)
        coEvery { paymentAccountStatusFetcher.invoke(USER_WALLET_ID) } returns Unit.right()

        val result = useCase(order, USER_WALLET_ID)

        assertThat(result).isTrue()
        coVerify(exactly = 1) { paymentAccountStatusFetcher.invoke(USER_WALLET_ID) }
        coVerify(exactly = 0) { cardDetailsRepository.getOrderInfo(any(), any()) }
    }

    @Test
    fun `GIVEN order already CANCELED WHEN invoke THEN returns false without polling`() = runTest {
        val order = TangemPayOrderInfo(ORDER_ID, OrderStatus.CANCELED)
        coEvery { paymentAccountStatusFetcher.invoke(USER_WALLET_ID) } returns Unit.right()

        val result = useCase(order, USER_WALLET_ID)

        assertThat(result).isFalse()
        coVerify(exactly = 1) { paymentAccountStatusFetcher.invoke(USER_WALLET_ID) }
        coVerify(exactly = 0) { cardDetailsRepository.getOrderInfo(any(), any()) }
    }

    @Test
    fun `GIVEN processing order WHEN poll returns COMPLETED THEN returns true and fetches status`() = runTest {
        val order = TangemPayOrderInfo(ORDER_ID, OrderStatus.PROCESSING)
        coEvery {
            cardDetailsRepository.getOrderInfo(USER_WALLET_ID, ORDER_ID)
        } returns TangemPayOrderInfo(ORDER_ID, OrderStatus.COMPLETED).right()
        coEvery { paymentAccountStatusFetcher.invoke(USER_WALLET_ID) } returns Unit.right()

        val result = useCase(order, USER_WALLET_ID)

        assertThat(result).isTrue()
        coVerify(exactly = 1) { cardDetailsRepository.getOrderInfo(USER_WALLET_ID, ORDER_ID) }
        coVerify(exactly = 1) { paymentAccountStatusFetcher.invoke(USER_WALLET_ID) }
    }

    @Test
    fun `GIVEN processing order WHEN poll returns CANCELED THEN returns false and fetches status`() = runTest {
        val order = TangemPayOrderInfo(ORDER_ID, OrderStatus.PROCESSING)
        coEvery {
            cardDetailsRepository.getOrderInfo(USER_WALLET_ID, ORDER_ID)
        } returns TangemPayOrderInfo(ORDER_ID, OrderStatus.CANCELED).right()
        coEvery { paymentAccountStatusFetcher.invoke(USER_WALLET_ID) } returns Unit.right()

        val result = useCase(order, USER_WALLET_ID)

        assertThat(result).isFalse()
        coVerify(exactly = 1) { paymentAccountStatusFetcher.invoke(USER_WALLET_ID) }
    }

    @Test
    fun `GIVEN new order WHEN getOrderInfo fails once then returns COMPLETED THEN returns true after two polls`() = runTest {
        val order = TangemPayOrderInfo(ORDER_ID, OrderStatus.NEW)
        coEvery {
            cardDetailsRepository.getOrderInfo(USER_WALLET_ID, ORDER_ID)
        } returnsMany listOf(
            VisaApiError.Unspecified.left(),
            TangemPayOrderInfo(ORDER_ID, OrderStatus.COMPLETED).right(),
        )
        coEvery { paymentAccountStatusFetcher.invoke(USER_WALLET_ID) } returns Unit.right()

        val result = useCase(order, USER_WALLET_ID)

        assertThat(result).isTrue()
        coVerify(exactly = 2) { cardDetailsRepository.getOrderInfo(USER_WALLET_ID, ORDER_ID) }
    }

    @Test
    fun `GIVEN processing order WHEN multiple non-final polls then COMPLETED THEN returns true after all polls`() = runTest {
        val order = TangemPayOrderInfo(ORDER_ID, OrderStatus.PROCESSING)
        coEvery {
            cardDetailsRepository.getOrderInfo(USER_WALLET_ID, ORDER_ID)
        } returnsMany listOf(
            TangemPayOrderInfo(ORDER_ID, OrderStatus.PROCESSING).right(),
            TangemPayOrderInfo(ORDER_ID, OrderStatus.NEW).right(),
            TangemPayOrderInfo(ORDER_ID, OrderStatus.COMPLETED).right(),
        )
        coEvery { paymentAccountStatusFetcher.invoke(USER_WALLET_ID) } returns Unit.right()

        val result = useCase(order, USER_WALLET_ID)

        assertThat(result).isTrue()
        coVerify(exactly = 3) { cardDetailsRepository.getOrderInfo(USER_WALLET_ID, ORDER_ID) }
        coVerify(exactly = 1) { paymentAccountStatusFetcher.invoke(USER_WALLET_ID) }
    }

    @Test
    fun `GIVEN order already being polled WHEN invoke again for same order THEN returns false without a second poll`() =
        runTest {
            // Arrange — first poller never reaches a terminal status, so it keeps polling.
            val order = TangemPayOrderInfo(ORDER_ID, OrderStatus.PROCESSING)
            coEvery { cardDetailsRepository.getOrderInfo(USER_WALLET_ID, ORDER_ID) } returns
                TangemPayOrderInfo(ORDER_ID, OrderStatus.PROCESSING).right()

            // Act — start the first poller, let it register the order and park in its poll delay,
            // then invoke again for the same order.
            val firstPoller = launch { useCase(order, USER_WALLET_ID) }
            runCurrent()
            val secondResult = useCase(order, USER_WALLET_ID)

            // Assert — the duplicate invoke is a no-op (no extra getOrderInfo, no status fetch).
            assertThat(secondResult).isFalse()
            coVerify(exactly = 1) { cardDetailsRepository.getOrderInfo(USER_WALLET_ID, ORDER_ID) }
            coVerify(exactly = 0) { paymentAccountStatusFetcher.invoke(USER_WALLET_ID) }

            firstPoller.cancel()
        }

    private companion object {
        val USER_WALLET_ID = UserWalletId("aabbcc112233")
        const val ORDER_ID = "order-test-1"
    }
}