package com.tangem.domain.pay.usecase

import arrow.core.left
import arrow.core.right
import com.google.common.truth.Truth.assertThat
import com.tangem.domain.models.wallet.UserWalletId
import com.tangem.domain.pay.model.OrderStatus
import com.tangem.domain.pay.model.TangemPayOrderInfo
import com.tangem.domain.pay.model.TangemPayPendingOrder
import com.tangem.domain.pay.repository.TangemPayCardDetailsRepository
import com.tangem.domain.pay.TangemPayOrderPollingScheduler
import com.tangem.domain.visa.error.VisaApiError
import io.mockk.coEvery
import io.mockk.coVerify
import io.mockk.mockk
import kotlinx.coroutines.CompletableDeferred
import kotlinx.coroutines.test.runTest
import org.junit.jupiter.api.Test

internal class ChangeCardFrozenStateUseCaseTest {

    private val cardDetailsRepository: TangemPayCardDetailsRepository = mockk(relaxUnitFun = true)
    private val pollingScheduler: TangemPayOrderPollingScheduler = mockk(relaxed = true)

    private val useCase = ChangeCardFrozenStateUseCase(
        cardDetailsRepository = cardDetailsRepository,
        pollingScheduler = pollingScheduler,
    )

    @Test
    fun `GIVEN freezeCard fails WHEN invoke with isFreezing=true THEN returns Left and does not schedule`() = runTest {
        coEvery {
            cardDetailsRepository.freezeCard(USER_WALLET_ID, CARD_ID)
        } returns VisaApiError.Unspecified.left()

        val result = useCase(USER_WALLET_ID, CARD_ID, isFreezing = true)

        assertThat(result.isLeft()).isTrue()
        coVerify(exactly = 0) { pollingScheduler.scheduleOrderAsync(any()) }
    }

    @Test
    fun `GIVEN freeze returns CANCELED order WHEN invoke THEN returns Left and does not schedule`() = runTest {
        val order = TangemPayOrderInfo(ORDER_ID, OrderStatus.CANCELED)
        coEvery { cardDetailsRepository.freezeCard(USER_WALLET_ID, CARD_ID) } returns order.right()

        val result = useCase(USER_WALLET_ID, CARD_ID, isFreezing = true)

        assertThat(result.isLeft()).isTrue()
        coVerify(exactly = 0) { pollingScheduler.scheduleOrderAsync(any()) }
    }

    @Test
    fun `GIVEN freeze succeeds and polling completes WHEN invoke THEN schedules FREEZE order and returns Right`() =
        runTest {
            val order = TangemPayOrderInfo(ORDER_ID, OrderStatus.PROCESSING)
            val expectedPendingOrder = TangemPayPendingOrder(
                orderId = ORDER_ID,
                userWalletId = USER_WALLET_ID,
                cardId = CARD_ID,
                type = TangemPayPendingOrder.Type.FREEZE,
                status = OrderStatus.PROCESSING,
            )
            coEvery { cardDetailsRepository.freezeCard(USER_WALLET_ID, CARD_ID) } returns order.right()
            coEvery { pollingScheduler.scheduleOrderAsync(expectedPendingOrder) } returns CompletableDeferred(true)

            val result = useCase(USER_WALLET_ID, CARD_ID, isFreezing = true)

            assertThat(result.isRight()).isTrue()
            coVerify(exactly = 1) { pollingScheduler.scheduleOrderAsync(expectedPendingOrder) }
        }

    @Test
    fun `GIVEN freeze succeeds but polling is canceled WHEN invoke THEN returns Left`() = runTest {
        val order = TangemPayOrderInfo(ORDER_ID, OrderStatus.PROCESSING)
        coEvery { cardDetailsRepository.freezeCard(USER_WALLET_ID, CARD_ID) } returns order.right()
        coEvery { pollingScheduler.scheduleOrderAsync(any()) } returns CompletableDeferred(false)

        val result = useCase(USER_WALLET_ID, CARD_ID, isFreezing = true)

        assertThat(result.isLeft()).isTrue()
    }

    @Test
    fun `GIVEN unfreeze succeeds and polling completes WHEN invoke THEN schedules UNFREEZE order and returns Right`() =
        runTest {
            val order = TangemPayOrderInfo(ORDER_ID, OrderStatus.NEW)
            val expectedPendingOrder = TangemPayPendingOrder(
                orderId = ORDER_ID,
                userWalletId = USER_WALLET_ID,
                cardId = CARD_ID,
                type = TangemPayPendingOrder.Type.UNFREEZE,
                status = OrderStatus.NEW,
            )
            coEvery { cardDetailsRepository.unfreezeCard(USER_WALLET_ID, CARD_ID) } returns order.right()
            coEvery { pollingScheduler.scheduleOrderAsync(expectedPendingOrder) } returns CompletableDeferred(true)

            val result = useCase(USER_WALLET_ID, CARD_ID, isFreezing = false)

            assertThat(result.isRight()).isTrue()
            coVerify(exactly = 1) { pollingScheduler.scheduleOrderAsync(expectedPendingOrder) }
        }

    private companion object {
        val USER_WALLET_ID = UserWalletId("aabbcc112233")
        const val CARD_ID = "card-test-id"
        const val ORDER_ID = "order-test-1"
    }
}