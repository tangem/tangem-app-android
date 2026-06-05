package com.tangem.domain.pay.usecase

import arrow.core.left
import arrow.core.right
import com.google.common.truth.Truth.assertThat
import com.tangem.domain.models.wallet.UserWalletId
import com.tangem.domain.pay.model.OrderStatus
import com.tangem.domain.pay.model.TangemPayOrderInfo
import com.tangem.domain.pay.repository.TangemPayCardDetailsRepository
import com.tangem.domain.visa.error.VisaApiError
import io.mockk.*
import kotlinx.coroutines.test.runTest
import org.junit.jupiter.api.Test

internal class StartTangemPayOrderPollingUseCaseTest {

    private val cardDetailsRepository: TangemPayCardDetailsRepository = mockk()

    private val useCase = StartTangemPayOrderPollingUseCase(
        cardDetailsRepository = cardDetailsRepository,
    )

    @Test
    fun `GIVEN order already COMPLETED WHEN invoke THEN returns true without polling`() = runTest {
        val order = TangemPayOrderInfo(ORDER_ID, OrderStatus.COMPLETED)

        val result = useCase(order, USER_WALLET_ID)

        assertThat(result).isTrue()
        coVerify(exactly = 0) { cardDetailsRepository.getOrderInfo(any(), any()) }
    }

    @Test
    fun `GIVEN order already CANCELED WHEN invoke THEN returns false without polling`() = runTest {
        val order = TangemPayOrderInfo(ORDER_ID, OrderStatus.CANCELED)

        val result = useCase(order, USER_WALLET_ID)

        assertThat(result).isFalse()
        coVerify(exactly = 0) { cardDetailsRepository.getOrderInfo(any(), any()) }
    }

    @Test
    fun `GIVEN processing order WHEN poll returns COMPLETED THEN returns true`() = runTest {
        val order = TangemPayOrderInfo(ORDER_ID, OrderStatus.PROCESSING)
        coEvery {
            cardDetailsRepository.getOrderInfo(USER_WALLET_ID, ORDER_ID)
        } returns TangemPayOrderInfo(ORDER_ID, OrderStatus.COMPLETED).right()

        val result = useCase(order, USER_WALLET_ID)

        assertThat(result).isTrue()
        coVerify(exactly = 1) { cardDetailsRepository.getOrderInfo(USER_WALLET_ID, ORDER_ID) }
    }

    @Test
    fun `GIVEN processing order WHEN poll returns CANCELED THEN returns false`() = runTest {
        val order = TangemPayOrderInfo(ORDER_ID, OrderStatus.PROCESSING)
        coEvery {
            cardDetailsRepository.getOrderInfo(USER_WALLET_ID, ORDER_ID)
        } returns TangemPayOrderInfo(ORDER_ID, OrderStatus.CANCELED).right()

        val result = useCase(order, USER_WALLET_ID)

        assertThat(result).isFalse()
        coVerify(exactly = 1) { cardDetailsRepository.getOrderInfo(USER_WALLET_ID, ORDER_ID) }
    }

    @Test
    fun `GIVEN new order WHEN getOrderInfo fails once then returns COMPLETED THEN returns true after two polls`() =
        runTest {
            val order = TangemPayOrderInfo(ORDER_ID, OrderStatus.NEW)
            coEvery {
                cardDetailsRepository.getOrderInfo(USER_WALLET_ID, ORDER_ID)
            } returnsMany listOf(
                VisaApiError.Unspecified.left(),
                TangemPayOrderInfo(ORDER_ID, OrderStatus.COMPLETED).right(),
            )

            val result = useCase(order, USER_WALLET_ID)

            assertThat(result).isTrue()
            coVerify(exactly = 2) { cardDetailsRepository.getOrderInfo(USER_WALLET_ID, ORDER_ID) }
        }

    @Test
    fun `GIVEN processing order WHEN multiple non-final polls then COMPLETED THEN returns true after all polls`() =
        runTest {
            val order = TangemPayOrderInfo(ORDER_ID, OrderStatus.PROCESSING)
            coEvery {
                cardDetailsRepository.getOrderInfo(USER_WALLET_ID, ORDER_ID)
            } returnsMany listOf(
                TangemPayOrderInfo(ORDER_ID, OrderStatus.PROCESSING).right(),
                TangemPayOrderInfo(ORDER_ID, OrderStatus.NEW).right(),
                TangemPayOrderInfo(ORDER_ID, OrderStatus.COMPLETED).right(),
            )

            val result = useCase(order, USER_WALLET_ID)

            assertThat(result).isTrue()
            coVerify(exactly = 3) { cardDetailsRepository.getOrderInfo(USER_WALLET_ID, ORDER_ID) }
        }

    private companion object {
        val USER_WALLET_ID = UserWalletId("aabbcc112233")
        const val ORDER_ID = "order-test-1"
    }
}