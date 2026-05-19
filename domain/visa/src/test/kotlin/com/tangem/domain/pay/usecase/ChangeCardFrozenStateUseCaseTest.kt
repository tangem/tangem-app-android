package com.tangem.domain.pay.usecase

import arrow.core.left
import arrow.core.right
import com.google.common.truth.Truth.assertThat
import com.tangem.common.test.TestAppCoroutineScope
import com.tangem.domain.models.wallet.UserWalletId
import com.tangem.domain.pay.model.OrderStatus
import com.tangem.domain.pay.model.TangemPayOrderInfo
import com.tangem.domain.pay.repository.TangemPayCardDetailsRepository
import com.tangem.domain.visa.error.VisaApiError
import com.tangem.domain.visa.model.TangemPayCardFrozenState
import io.mockk.coEvery
import io.mockk.coVerify
import io.mockk.coVerifyOrder
import io.mockk.mockk
import kotlinx.coroutines.test.TestScope
import kotlinx.coroutines.test.runTest
import org.junit.jupiter.api.Test

internal class ChangeCardFrozenStateUseCaseTest {

    private val cardDetailsRepository: TangemPayCardDetailsRepository = mockk(relaxUnitFun = true)
    private val startPollingUseCase: StartTangemPayOrderPollingUseCase = mockk()

    @Test
    fun `GIVEN freezeCard fails WHEN invoke with isFreezing=true THEN sets Pending then Unfrozen and returns Left`() =
        runTest {
            val useCase = createUseCase()
            coEvery {
                cardDetailsRepository.freezeCard(USER_WALLET_ID, CARD_ID)
            } returns VisaApiError.Unspecified.left()

            val result = useCase(USER_WALLET_ID, CARD_ID, isFreezing = true)

            assertThat(result.isLeft()).isTrue()
            coVerifyOrder {
                cardDetailsRepository.setCardFrozenState(CARD_ID, TangemPayCardFrozenState.Pending)
                cardDetailsRepository.setCardFrozenState(CARD_ID, TangemPayCardFrozenState.Unfrozen)
            }
            coVerify(exactly = 0) { startPollingUseCase(any(), any()) }
        }

    @Test
    fun `GIVEN unfreezeCard fails WHEN invoke with isFreezing=false THEN sets Pending then Frozen and returns Left`() =
        runTest {
            val useCase = createUseCase()
            coEvery {
                cardDetailsRepository.unfreezeCard(USER_WALLET_ID, CARD_ID)
            } returns VisaApiError.Unspecified.left()

            val result = useCase(USER_WALLET_ID, CARD_ID, isFreezing = false)

            assertThat(result.isLeft()).isTrue()
            coVerifyOrder {
                cardDetailsRepository.setCardFrozenState(CARD_ID, TangemPayCardFrozenState.Pending)
                cardDetailsRepository.setCardFrozenState(CARD_ID, TangemPayCardFrozenState.Frozen)
            }
            coVerify(exactly = 0) { startPollingUseCase(any(), any()) }
        }

    @Test
    fun `GIVEN freeze succeeds and order COMPLETED WHEN invoke with isFreezing=true THEN sets Frozen and returns Right`() =
        runTest {
            val useCase = createUseCase()
            val order = TangemPayOrderInfo(ORDER_ID, OrderStatus.COMPLETED)
            coEvery { cardDetailsRepository.freezeCard(USER_WALLET_ID, CARD_ID) } returns order.right()
            coEvery { startPollingUseCase(order, USER_WALLET_ID) } returns true

            val result = useCase(USER_WALLET_ID, CARD_ID, isFreezing = true)

            assertThat(result.isRight()).isTrue()
            coVerifyOrder {
                cardDetailsRepository.setCardFrozenState(CARD_ID, TangemPayCardFrozenState.Pending)
                cardDetailsRepository.setCardFrozenState(CARD_ID, TangemPayCardFrozenState.Frozen)
            }
        }

    @Test
    fun `GIVEN unfreeze succeeds and order COMPLETED WHEN invoke with isFreezing=false THEN sets Unfrozen and returns Right`() =
        runTest {
            val useCase = createUseCase()
            val order = TangemPayOrderInfo(ORDER_ID, OrderStatus.COMPLETED)
            coEvery { cardDetailsRepository.unfreezeCard(USER_WALLET_ID, CARD_ID) } returns order.right()
            coEvery { startPollingUseCase(order, USER_WALLET_ID) } returns true

            val result = useCase(USER_WALLET_ID, CARD_ID, isFreezing = false)

            assertThat(result.isRight()).isTrue()
            coVerifyOrder {
                cardDetailsRepository.setCardFrozenState(CARD_ID, TangemPayCardFrozenState.Pending)
                cardDetailsRepository.setCardFrozenState(CARD_ID, TangemPayCardFrozenState.Unfrozen)
            }
        }

    private fun TestScope.createUseCase() = ChangeCardFrozenStateUseCase(
        cardDetailsRepository = cardDetailsRepository,
        startTangemPayOrderPollingUseCase = startPollingUseCase,
        appCoroutineScope = TestAppCoroutineScope(this),
    )

    private companion object {
        val USER_WALLET_ID = UserWalletId("aabbcc112233")
        const val CARD_ID = "card-test-id"
        const val ORDER_ID = "order-test-1"
    }
}