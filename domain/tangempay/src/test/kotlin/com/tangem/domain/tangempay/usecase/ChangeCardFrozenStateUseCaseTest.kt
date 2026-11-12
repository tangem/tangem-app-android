package com.tangem.domain.tangempay.usecase

import arrow.core.left
import arrow.core.right
import com.google.common.truth.Truth.assertThat
import com.tangem.domain.models.tangempay.TangemPayCardFrozenState
import com.tangem.domain.models.wallet.UserWalletId
import com.tangem.domain.tangempay.error.TangemPayApiError
import com.tangem.domain.tangempay.model.OrderDetails
import com.tangem.domain.tangempay.model.OrderStatus
import com.tangem.domain.tangempay.model.OrderStep
import com.tangem.domain.tangempay.model.TangemPayOrder
import com.tangem.domain.tangempay.model.TangemPayOrderInfo
import com.tangem.domain.tangempay.order.OrderLaunch
import com.tangem.domain.tangempay.order.OrderPollingState
import com.tangem.domain.tangempay.usecase.order.StartOrderUseCase
import com.tangem.domain.tangempay.order.RunningOrder
import com.tangem.domain.tangempay.order.OrderRef
import com.tangem.domain.tangempay.repository.TangemPayCardDetailsRepository
import com.tangem.features.tangempay.TangemPayFeatureToggles
import com.tangem.test.core.TestAppCoroutineScope
import io.mockk.clearMocks
import io.mockk.coEvery
import io.mockk.coVerify
import io.mockk.coVerifyOrder
import io.mockk.every
import io.mockk.mockk
import kotlinx.coroutines.flow.flowOf
import kotlinx.coroutines.test.TestScope
import kotlinx.coroutines.test.runTest
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Test
import org.junit.jupiter.params.provider.MethodSource
import org.junit.jupiter.params.ParameterizedTest
import org.junit.jupiter.api.TestInstance

@TestInstance(TestInstance.Lifecycle.PER_CLASS)
internal class ChangeCardFrozenStateUseCaseTest {

    private val cardDetailsRepository: TangemPayCardDetailsRepository = mockk(relaxUnitFun = true)
    private val startOrderUseCase: StartOrderUseCase = mockk()
    private val startTangemPayOrderPollingUseCase: StartTangemPayOrderPollingUseCase = mockk()
    private val tangemPayFeatureToggles: TangemPayFeatureToggles = mockk()

    @BeforeEach
    fun resetMocks() {
        clearMocks(cardDetailsRepository, startOrderUseCase, startTangemPayOrderPollingUseCase, tangemPayFeatureToggles)
        every { tangemPayFeatureToggles.isOrderFrameworkEnabled } returns true
    }

    @Test
    fun `GIVEN freezeCard fails WHEN invoke THEN returns Left without starting an order`() = runTest {
        // Arrange
        val useCase = createUseCase()
        coEvery {
            cardDetailsRepository.freezeCard(USER_WALLET_ID, CARD_ID)
        } returns TangemPayApiError.Unspecified.left()

        // Act
        val result = useCase(USER_WALLET_ID, CARD_ID, isFreezing = true)

        // Assert
        assertThat(result.leftOrNull()).isEqualTo(TangemPayApiError.Unspecified)
        coVerify(exactly = 0) { startOrderUseCase(any(), any()) }
        coVerify(exactly = 0) { cardDetailsRepository.setCardFrozenState(any(), any()) }
    }

    @Test
    fun `GIVEN freeze order completes WHEN invoke with isFreezing=true THEN polls the order and returns Right`() =
        runTest {
            // Arrange
            val useCase = createUseCase()
            coEvery { cardDetailsRepository.freezeCard(USER_WALLET_ID, CARD_ID) } returns ORDER_INFO.right()
            coEvery { startOrderUseCase(USER_WALLET_ID, LAUNCH) } returns runningOrder(
                finalState = OrderPollingState.Loaded(order(TangemPayOrder.Status.COMPLETED)),
            ).right()

            // Act
            val result = useCase(USER_WALLET_ID, CARD_ID, isFreezing = true)

            // Assert
            assertThat(result.isRight()).isTrue()
            coVerify(exactly = 1) { startOrderUseCase(USER_WALLET_ID, LAUNCH) }
            coVerify(exactly = 0) { cardDetailsRepository.setCardFrozenState(any(), any()) }
        }

    @Test
    fun `GIVEN unfreeze order completes WHEN invoke with isFreezing=false THEN polls the order and returns Right`() =
        runTest {
            // Arrange
            val useCase = createUseCase()
            coEvery { cardDetailsRepository.unfreezeCard(USER_WALLET_ID, CARD_ID) } returns ORDER_INFO.right()
            coEvery { startOrderUseCase(USER_WALLET_ID, LAUNCH) } returns runningOrder(
                finalState = OrderPollingState.Loaded(order(TangemPayOrder.Status.COMPLETED)),
            ).right()

            // Act
            val result = useCase(USER_WALLET_ID, CARD_ID, isFreezing = false)

            // Assert
            assertThat(result.isRight()).isTrue()
            coVerify(exactly = 1) { startOrderUseCase(USER_WALLET_ID, LAUNCH) }
            coVerify(exactly = 0) { cardDetailsRepository.setCardFrozenState(any(), any()) }
        }

    @ParameterizedTest
    @MethodSource("provideNotCompletedFinalStates")
    fun `GIVEN order ends not completed WHEN invoke THEN returns Left`(finalState: OrderPollingState) =
        runTest {
            // Arrange
            val useCase = createUseCase()
            coEvery { cardDetailsRepository.freezeCard(USER_WALLET_ID, CARD_ID) } returns ORDER_INFO.right()
            coEvery { startOrderUseCase(USER_WALLET_ID, LAUNCH) } returns runningOrder(finalState).right()

            // Act
            val result = useCase(USER_WALLET_ID, CARD_ID, isFreezing = true)

            // Assert
            assertThat(result.leftOrNull()).isEqualTo(TangemPayApiError.Unspecified)
            coVerify(exactly = 0) { cardDetailsRepository.setCardFrozenState(any(), any()) }
        }

    @Test
    fun `GIVEN start order fails WHEN invoke THEN returns its error`() = runTest {
        // Arrange
        val useCase = createUseCase()
        coEvery { cardDetailsRepository.freezeCard(USER_WALLET_ID, CARD_ID) } returns ORDER_INFO.right()
        coEvery { startOrderUseCase(USER_WALLET_ID, LAUNCH) } returns TangemPayApiError.OrderNotFound.left()

        // Act
        val result = useCase(USER_WALLET_ID, CARD_ID, isFreezing = true)

        // Assert
        assertThat(result.leftOrNull()).isEqualTo(TangemPayApiError.OrderNotFound)
        coVerify(exactly = 0) { cardDetailsRepository.setCardFrozenState(any(), any()) }
    }

    @ParameterizedTest
    @MethodSource("provideLegacyRequestFailedModels")
    fun `GIVEN order framework disabled and request fails WHEN invoke THEN sets Pending then rolls back`(
        model: LegacyFrozenStateModel,
    ) = runTest {
        // Arrange
        every { tangemPayFeatureToggles.isOrderFrameworkEnabled } returns false
        val useCase = createUseCase()
        coEvery {
            cardDetailsRepository.freezeOrUnfreeze(model.isFreezing)
        } returns TangemPayApiError.Unspecified.left()

        // Act
        val result = useCase(USER_WALLET_ID, CARD_ID, isFreezing = model.isFreezing)

        // Assert
        assertThat(result.isLeft()).isTrue()
        coVerify(exactly = 0) { startTangemPayOrderPollingUseCase(any(), any()) }
        coVerifyOrder {
            cardDetailsRepository.setCardFrozenState(CARD_ID, TangemPayCardFrozenState.Pending)
            cardDetailsRepository.setCardFrozenState(CARD_ID, model.expectedState)
        }
    }

    @ParameterizedTest
    @MethodSource("provideLegacyCompletedModels")
    fun `GIVEN order framework disabled and order completes WHEN invoke THEN sets Pending then target state`(
        model: LegacyFrozenStateModel,
    ) = runTest {
        // Arrange
        every { tangemPayFeatureToggles.isOrderFrameworkEnabled } returns false
        val useCase = createUseCase()
        coEvery { cardDetailsRepository.freezeOrUnfreeze(model.isFreezing) } returns ORDER_INFO.right()
        coEvery { startTangemPayOrderPollingUseCase(ORDER_INFO, USER_WALLET_ID) } returns true

        // Act
        val result = useCase(USER_WALLET_ID, CARD_ID, isFreezing = model.isFreezing)

        // Assert
        assertThat(result.isRight()).isTrue()
        coVerify(exactly = 0) { startOrderUseCase(any(), any()) }
        coVerifyOrder {
            cardDetailsRepository.setCardFrozenState(CARD_ID, TangemPayCardFrozenState.Pending)
            cardDetailsRepository.setCardFrozenState(CARD_ID, model.expectedState)
        }
    }

    @ParameterizedTest
    @MethodSource("provideLegacyRequestFailedModels")
    fun `GIVEN order framework disabled and order fails WHEN invoke THEN sets Pending then rolls back`(
        model: LegacyFrozenStateModel,
    ) = runTest {
        // Arrange
        every { tangemPayFeatureToggles.isOrderFrameworkEnabled } returns false
        val useCase = createUseCase()
        coEvery { cardDetailsRepository.freezeOrUnfreeze(model.isFreezing) } returns ORDER_INFO.right()
        coEvery { startTangemPayOrderPollingUseCase(ORDER_INFO, USER_WALLET_ID) } returns false

        // Act
        val result = useCase(USER_WALLET_ID, CARD_ID, isFreezing = model.isFreezing)

        // Assert
        assertThat(result.isLeft()).isTrue()
        coVerify(exactly = 0) { startOrderUseCase(any(), any()) }
        coVerifyOrder {
            cardDetailsRepository.setCardFrozenState(CARD_ID, TangemPayCardFrozenState.Pending)
            cardDetailsRepository.setCardFrozenState(CARD_ID, model.expectedState)
        }
    }

    private fun provideLegacyRequestFailedModels() = listOf(
        LegacyFrozenStateModel(isFreezing = true, expectedState = TangemPayCardFrozenState.Unfrozen),
        LegacyFrozenStateModel(isFreezing = false, expectedState = TangemPayCardFrozenState.Frozen),
    )

    private fun provideLegacyCompletedModels() = listOf(
        LegacyFrozenStateModel(isFreezing = true, expectedState = TangemPayCardFrozenState.Frozen),
        LegacyFrozenStateModel(isFreezing = false, expectedState = TangemPayCardFrozenState.Unfrozen),
    )

    private suspend fun TangemPayCardDetailsRepository.freezeOrUnfreeze(isFreezing: Boolean) =
        if (isFreezing) freezeCard(USER_WALLET_ID, CARD_ID) else unfreezeCard(USER_WALLET_ID, CARD_ID)

    private fun provideNotCompletedFinalStates() = listOf(
        OrderPollingState.Loaded(order(TangemPayOrder.Status.CANCELED)),
        OrderPollingState.NotFound,
        OrderPollingState.Stopped,
    )

    private fun TestScope.createUseCase() = ChangeCardFrozenStateUseCase(
        cardDetailsRepository = cardDetailsRepository,
        startOrderUseCase = startOrderUseCase,
        startTangemPayOrderPollingUseCase = startTangemPayOrderPollingUseCase,
        tangemPayFeatureToggles = tangemPayFeatureToggles,
        appCoroutineScope = TestAppCoroutineScope(this),
    )

    private fun runningOrder(finalState: OrderPollingState) = RunningOrder(
        ref = OrderRef(userWalletId = USER_WALLET_ID, id = ORDER_ID),
        state = flowOf(OrderPollingState.Pending, finalState),
    )

    private fun order(status: TangemPayOrder.Status) = TangemPayOrder(
        id = ORDER_ID,
        status = status,
        step = OrderStep.UNKNOWN,
        details = OrderDetails.CardFreeze(productInstanceId = "pi-1"),
    )

    internal data class LegacyFrozenStateModel(
        val isFreezing: Boolean,
        val expectedState: TangemPayCardFrozenState,
    )

    private companion object {
        val USER_WALLET_ID = UserWalletId("aabbcc112233")
        const val CARD_ID = "card-test-id"
        const val ORDER_ID = "order-test-1"
        val ORDER_INFO = TangemPayOrderInfo(ORDER_ID, OrderStatus.PROCESSING)
        val LAUNCH = OrderLaunch.Id(orderId = ORDER_ID)
    }
}