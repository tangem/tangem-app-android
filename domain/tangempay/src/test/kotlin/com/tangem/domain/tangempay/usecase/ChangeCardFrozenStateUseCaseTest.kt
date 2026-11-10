package com.tangem.domain.tangempay.usecase

import arrow.core.left
import arrow.core.right
import com.google.common.truth.Truth.assertThat
import com.tangem.domain.models.tangempay.TangemPayCardFrozenState
import com.tangem.domain.models.wallet.UserWalletId
import com.tangem.domain.tangempay.model.OrderStatus
import com.tangem.domain.tangempay.model.TangemPayOrderInfo
import com.tangem.domain.tangempay.repository.TangemPayCardDetailsRepository
import com.tangem.domain.tangempay.error.TangemPayApiError
import com.tangem.test.core.ProvideTestModels
import com.tangem.test.core.TestAppCoroutineScope
import io.mockk.clearMocks
import io.mockk.coEvery
import io.mockk.coVerify
import io.mockk.coVerifyOrder
import io.mockk.mockk
import kotlinx.coroutines.test.TestScope
import kotlinx.coroutines.test.runTest
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.TestInstance
import org.junit.jupiter.params.ParameterizedTest
import org.junit.jupiter.params.provider.MethodSource

@TestInstance(TestInstance.Lifecycle.PER_CLASS)
internal class ChangeCardFrozenStateUseCaseTest {

    private val cardDetailsRepository: TangemPayCardDetailsRepository = mockk(relaxUnitFun = true)
    private val startPollingUseCase: StartTangemPayOrderPollingUseCase = mockk()

    @BeforeEach
    fun resetMocks() {
        clearMocks(cardDetailsRepository, startPollingUseCase)
    }

    @ParameterizedTest
    @ProvideTestModels
    fun `GIVEN freeze or unfreeze request fails WHEN invoke THEN sets Pending then rolls back and returns Left`(
        model: FrozenStateModel,
    ) = runTest {
        // Arrange
        val useCase = createUseCase()
        coEvery { cardDetailsRepository.freezeOrUnfreeze(model.isFreezing) } returns TangemPayApiError.Unspecified.left()

        // Act
        val result = useCase(USER_WALLET_ID, CARD_ID, isFreezing = model.isFreezing)

        // Assert
        assertThat(result.isLeft()).isTrue()
        coVerifyOrder {
            cardDetailsRepository.setCardFrozenState(CARD_ID, TangemPayCardFrozenState.Pending)
            cardDetailsRepository.setCardFrozenState(CARD_ID, model.expectedState)
        }
        coVerify(exactly = 0) { startPollingUseCase(any(), any(), any()) }
    }

    @ParameterizedTest
    @MethodSource("completedOrderModels")
    fun `GIVEN freeze or unfreeze succeeds and order COMPLETED WHEN invoke THEN sets Pending then target state and returns Right`(
        model: FrozenStateModel,
    ) = runTest {
        // Arrange
        val useCase = createUseCase()
        val order = TangemPayOrderInfo(ORDER_ID, OrderStatus.COMPLETED)
        coEvery { cardDetailsRepository.freezeOrUnfreeze(model.isFreezing) } returns order.right()
        coEvery { startPollingUseCase(order, USER_WALLET_ID, any()) } returns true

        // Act
        val result = useCase(USER_WALLET_ID, CARD_ID, isFreezing = model.isFreezing)

        // Assert
        assertThat(result.isRight()).isTrue()
        coVerifyOrder {
            cardDetailsRepository.setCardFrozenState(CARD_ID, TangemPayCardFrozenState.Pending)
            cardDetailsRepository.setCardFrozenState(CARD_ID, model.expectedState)
        }
    }

    private fun provideTestModels() = listOf(
        FrozenStateModel(isFreezing = true, expectedState = TangemPayCardFrozenState.Unfrozen),
        FrozenStateModel(isFreezing = false, expectedState = TangemPayCardFrozenState.Frozen),
    )

    private fun completedOrderModels() = listOf(
        FrozenStateModel(isFreezing = true, expectedState = TangemPayCardFrozenState.Frozen),
        FrozenStateModel(isFreezing = false, expectedState = TangemPayCardFrozenState.Unfrozen),
    )

    private suspend fun TangemPayCardDetailsRepository.freezeOrUnfreeze(isFreezing: Boolean) =
        if (isFreezing) freezeCard(USER_WALLET_ID, CARD_ID) else unfreezeCard(USER_WALLET_ID, CARD_ID)

    private fun TestScope.createUseCase() = ChangeCardFrozenStateUseCase(
        cardDetailsRepository = cardDetailsRepository,
        startTangemPayOrderPollingUseCase = startPollingUseCase,
        appCoroutineScope = TestAppCoroutineScope(this),
    )

    internal data class FrozenStateModel(val isFreezing: Boolean, val expectedState: TangemPayCardFrozenState)

    private companion object {
        val USER_WALLET_ID = UserWalletId("aabbcc112233")
        const val CARD_ID = "card-test-id"
        const val ORDER_ID = "order-test-1"
    }
}