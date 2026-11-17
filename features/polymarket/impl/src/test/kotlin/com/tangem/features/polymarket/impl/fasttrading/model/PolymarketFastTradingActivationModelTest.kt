package com.tangem.features.polymarket.impl.fasttrading.model

import arrow.core.left
import arrow.core.right
import com.google.common.truth.Truth.assertThat
import com.tangem.core.decompose.model.MutableParamsContainer
import com.tangem.core.decompose.navigation.Router
import com.tangem.core.decompose.ui.UiMessageSender
import com.tangem.core.res.R
import com.tangem.core.ui.extensions.resourceReference
import com.tangem.core.ui.message.EventMessage
import com.tangem.domain.models.wallet.UserWallet
import com.tangem.domain.models.wallet.UserWalletId
import com.tangem.domain.polymarket.model.PolymarketSessionKey
import com.tangem.domain.polymarket.model.PolymarketSessionKeyError
import com.tangem.domain.polymarket.model.PolymarketSessionKeyLifecycleError
import com.tangem.domain.polymarket.model.PolymarketSessionKeyStatus
import com.tangem.domain.polymarket.model.PolymarketSigningError
import com.tangem.domain.polymarket.sessionkey.operation.PolymarketSessionKeyOperation
import com.tangem.domain.polymarket.sessionkey.operation.PolymarketSessionKeyOperationFailure
import com.tangem.domain.polymarket.sessionkey.operation.PolymarketSessionKeyOperationOutcome
import com.tangem.domain.polymarket.sessionkey.operation.PolymarketSessionKeyOperationRequest
import com.tangem.domain.polymarket.sessionkey.operation.PolymarketSessionKeyOperationStage
import com.tangem.domain.polymarket.sessionkey.operation.PolymarketSessionKeyOperationStart
import com.tangem.domain.polymarket.sessionkey.operation.PolymarketSessionKeyOperationState
import com.tangem.domain.polymarket.usecase.ObservePolymarketSessionKeyOperationStateUseCase
import com.tangem.domain.polymarket.usecase.StartPolymarketSessionKeyOperationUseCase
import com.tangem.domain.wallets.usecase.GetUserWalletUseCase
import com.tangem.features.polymarket.impl.fasttrading.PolymarketFastTradingActivationComponent
import com.tangem.features.polymarket.impl.fasttrading.PolymarketFastTradingActivationComponent.Action
import com.tangem.test.core.createTestingCoroutineDispatcherProvider
import io.mockk.clearMocks
import io.mockk.every
import io.mockk.mockk
import io.mockk.verify
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.test.TestScope
import kotlinx.coroutines.test.advanceUntilIdle
import kotlinx.coroutines.test.runTest
import org.junit.jupiter.api.AfterEach
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Test
import org.junit.jupiter.params.ParameterizedTest
import org.junit.jupiter.params.provider.Arguments
import org.junit.jupiter.params.provider.MethodSource
import java.time.Instant

@OptIn(ExperimentalCoroutinesApi::class)
internal class PolymarketFastTradingActivationModelTest {

    private val router: Router = mockk(relaxed = true)
    private val startOperation: StartPolymarketSessionKeyOperationUseCase = mockk()
    private val messageSender: UiMessageSender = mockk()
    private val getUserWalletUseCase: GetUserWalletUseCase = mockk()
    private val observeOperationState: ObservePolymarketSessionKeyOperationStateUseCase = mockk()
    private val operationState = MutableStateFlow(PolymarketSessionKeyOperationState.IDLE)
    private val sentMessages = mutableListOf<EventMessage>()
    private var model: PolymarketFastTradingActivationModel? = null

    @BeforeEach
    fun setup() {
        clearMocks(router, startOperation, messageSender, getUserWalletUseCase, observeOperationState)
        operationState.value = PolymarketSessionKeyOperationState.IDLE
        sentMessages.clear()
        every { getUserWalletUseCase(USER_WALLET_ID) } returns mockk<UserWallet.Cold>().right()
        every { observeOperationState(USER_WALLET_ID) } returns operationState
        every { messageSender.send(capture(sentMessages)) } returns Unit
        every { startOperation(USER_WALLET_ID, ACTIVATE) } returns
            PolymarketSessionKeyOperationStart.Started(OPERATION_ID)
    }

    @AfterEach
    fun destroyModel() {
        model?.onDestroy()
    }

    @Test
    fun `GIVEN model created WHEN no click THEN state is idle`() = runTest {
        // Act
        val model = createModel()
        advanceUntilIdle()

        // Assert
        assertThat(model.uiState.value.isActivating).isFalse()
        assertThat(model.uiState.value.isFaqShown).isFalse()
    }

    @Test
    fun `GIVEN idle WHEN activate clicked THEN activation starts and state is activating`() = runTest {
        // Arrange
        val model = createModel()
        advanceUntilIdle()

        // Act
        model.uiState.value.onActivateClick()
        advanceUntilIdle()

        // Assert
        verify(exactly = 1) { startOperation(USER_WALLET_ID, ACTIVATE) }
        assertThat(model.uiState.value.isActivating).isTrue()
    }

    @Test
    fun `GIVEN another operation running WHEN activate clicked THEN busy toast is sent and state stays idle`() =
        runTest {
            // Arrange
            every { startOperation(USER_WALLET_ID, ACTIVATE) } returns PolymarketSessionKeyOperationStart.Busy
            val model = createModel()
            advanceUntilIdle()

            // Act
            model.uiState.value.onActivateClick()
            advanceUntilIdle()

            // Assert
            val expected = PolymarketSessionKeyMessagesFactory.create(PolymarketSessionKeyOperationStart.Busy)
            assertThat(sentMessages).containsExactly(expected)
            assertThat(model.uiState.value.isActivating).isFalse()
        }

    @Test
    fun `GIVEN activating WHEN activate clicked again THEN nothing starts`() = runTest {
        // Arrange
        val model = createModel()
        advanceUntilIdle()
        model.uiState.value.onActivateClick()
        advanceUntilIdle()

        // Act
        model.uiState.value.onActivateClick()
        advanceUntilIdle()

        // Assert
        verify(exactly = 1) { startOperation(any(), any()) }
    }

    @Test
    fun `GIVEN activating WHEN operation is signing THEN screen stays open`() = runTest {
        // Arrange
        val model = createModel()
        advanceUntilIdle()
        model.uiState.value.onActivateClick()

        // Act
        operationState.value = state(running = operation(PolymarketSessionKeyOperationStage.SIGNING))
        advanceUntilIdle()

        // Assert
        verify(exactly = 0) { router.pop() }
        assertThat(model.uiState.value.isActivating).isTrue()
    }

    @Test
    fun `GIVEN activating WHEN operation leaves signing stage THEN screen closes`() = runTest {
        // Arrange
        val model = createModel()
        advanceUntilIdle()
        model.uiState.value.onActivateClick()
        operationState.value = state(running = operation(PolymarketSessionKeyOperationStage.SIGNING))
        advanceUntilIdle()

        // Act
        operationState.value = state(running = operation(PolymarketSessionKeyOperationStage.AUTHORIZING))
        advanceUntilIdle()

        // Assert
        verify(exactly = 1) { router.pop() }
        assertThat(model.uiState.value.isActivating).isFalse()
    }

    @Test
    fun `GIVEN activating WHEN outcome with the id succeeds THEN screen closes`() = runTest {
        // Arrange
        val model = createModel()
        advanceUntilIdle()
        model.uiState.value.onActivateClick()
        advanceUntilIdle()

        // Act
        operationState.value = state(finished = outcome(OPERATION_ID, SESSION_KEY.right()))
        advanceUntilIdle()

        // Assert
        verify(exactly = 1) { router.pop() }
    }

    @Test
    fun `GIVEN activating WHEN outcome with the id fails THEN state is idle and screen stays`() = runTest {
        // Arrange
        val model = createModel()
        advanceUntilIdle()
        model.uiState.value.onActivateClick()
        operationState.value = state(running = operation(PolymarketSessionKeyOperationStage.SIGNING))
        advanceUntilIdle()

        // Act
        operationState.value = state(finished = outcome(OPERATION_ID, CANCELLED.left()))
        advanceUntilIdle()

        // Assert
        verify(exactly = 0) { router.pop() }
        assertThat(model.uiState.value.isActivating).isFalse()
    }

    @Test
    fun `GIVEN activating and operation seen running WHEN operation disappears without outcome THEN state is idle`() =
        runTest {
            // Arrange
            val model = createModel()
            advanceUntilIdle()
            model.uiState.value.onActivateClick()
            operationState.value = state(running = operation(PolymarketSessionKeyOperationStage.SIGNING))
            advanceUntilIdle()

            // Act
            operationState.value = state()
            advanceUntilIdle()

            // Assert
            verify(exactly = 0) { router.pop() }
            assertThat(model.uiState.value.isActivating).isFalse()
        }

    @Test
    fun `GIVEN activating and operation seen running WHEN another operation replaces it THEN state is idle`() =
        runTest {
            // Arrange
            val model = createModel()
            advanceUntilIdle()
            model.uiState.value.onActivateClick()
            operationState.value = state(running = operation(PolymarketSessionKeyOperationStage.SIGNING))
            advanceUntilIdle()

            // Act
            operationState.value = state(
                running = operation(PolymarketSessionKeyOperationStage.SIGNING, id = OPERATION_ID + 1),
            )
            advanceUntilIdle()

            // Assert
            verify(exactly = 0) { router.pop() }
            assertThat(model.uiState.value.isActivating).isFalse()
        }

    @Test
    fun `GIVEN activating and operation not seen yet WHEN stale emission arrives THEN state stays activating`() =
        runTest {
            // Arrange
            val model = createModel()
            advanceUntilIdle()
            model.uiState.value.onActivateClick()
            advanceUntilIdle()

            // Act
            operationState.value = state(
                running = operation(PolymarketSessionKeyOperationStage.SIGNING, id = OPERATION_ID + 1),
                finished = outcome(OPERATION_ID + 1, CANCELLED.left()),
            )
            advanceUntilIdle()

            // Assert
            verify(exactly = 0) { router.pop() }
            assertThat(model.uiState.value.isActivating).isTrue()
        }

    @ParameterizedTest
    @MethodSource("actions")
    fun `GIVEN an action WHEN confirm clicked THEN its request starts under its title`(
        action: Action,
        request: PolymarketSessionKeyOperationRequest,
        titleRes: Int,
    ) = runTest {
        // Arrange
        every { startOperation(USER_WALLET_ID, request) } returns
            PolymarketSessionKeyOperationStart.Started(OPERATION_ID)
        val model = createModel(action = action)
        advanceUntilIdle()

        // Act
        model.uiState.value.onActivateClick()
        advanceUntilIdle()

        // Assert
        verify(exactly = 1) { startOperation(USER_WALLET_ID, request) }
        assertThat(model.uiState.value.buttonText).isEqualTo(resourceReference(titleRes))
    }

    @Test
    fun `GIVEN a hot wallet WHEN model created THEN the button is hold to confirm`() = runTest {
        // Arrange
        every { getUserWalletUseCase(USER_WALLET_ID) } returns mockk<UserWallet.Hot>().right()

        // Act
        val model = createModel()
        advanceUntilIdle()

        // Assert
        assertThat(model.uiState.value.isHoldToConfirm).isTrue()
    }

    @Test
    fun `GIVEN a card wallet WHEN model created THEN the button is a regular one`() = runTest {
        // Act
        val model = createModel()
        advanceUntilIdle()

        // Assert
        assertThat(model.uiState.value.isHoldToConfirm).isFalse()
    }

    @Test
    fun `GIVEN operation seen signing WHEN it finishes successfully in one snapshot THEN screen closes`() = runTest {
        // Arrange
        val model = createModel()
        advanceUntilIdle()
        model.uiState.value.onActivateClick()
        operationState.value = state(running = operation(PolymarketSessionKeyOperationStage.SIGNING))
        advanceUntilIdle()

        // Act
        operationState.value = state(finished = outcome(OPERATION_ID, SESSION_KEY.right()))
        advanceUntilIdle()

        // Assert
        verify(exactly = 1) { router.pop() }
        assertThat(model.uiState.value.isActivating).isFalse()
    }

    @Test
    fun `GIVEN host acknowledged before the screen saw running WHEN operation failed THEN state is idle`() =
        runTest {
            // Arrange
            val model = createModel()
            advanceUntilIdle()

            // Act
            model.uiState.value.onActivateClick()
            operationState.value = state(lastOutcome = outcome(OPERATION_ID, CANCELLED.left()))
            advanceUntilIdle()

            // Assert
            verify(exactly = 0) { router.pop() }
            assertThat(model.uiState.value.isActivating).isFalse()
        }

    @Test
    fun `GIVEN host acknowledged before the screen saw running WHEN operation succeeded THEN screen closes`() =
        runTest {
            // Arrange
            val model = createModel()
            advanceUntilIdle()

            // Act
            model.uiState.value.onActivateClick()
            operationState.value = state(lastOutcome = outcome(OPERATION_ID, SESSION_KEY.right()))
            advanceUntilIdle()

            // Assert
            verify(exactly = 1) { router.pop() }
            assertThat(model.uiState.value.isActivating).isFalse()
        }

    @Test
    fun `GIVEN activation started WHEN a foreign operation succeeds THEN its outcome is ignored`() = runTest {
        // Arrange
        val model = createModel()
        advanceUntilIdle()
        model.uiState.value.onActivateClick()
        advanceUntilIdle()

        // Act
        operationState.value = state(finished = outcome(OPERATION_ID + 1, SESSION_KEY.right()))
        advanceUntilIdle()

        // Assert
        verify(exactly = 0) { router.pop() }
        assertThat(model.uiState.value.isActivating).isTrue()
    }

    @Test
    fun `GIVEN faq hidden WHEN faq clicked and dismissed THEN faq shown toggles`() = runTest {
        // Arrange
        val model = createModel()
        advanceUntilIdle()

        // Act
        model.uiState.value.onFaqClick()
        val shown = model.uiState.value.isFaqShown
        model.uiState.value.onFaqDismiss()
        val hidden = model.uiState.value.isFaqShown

        // Assert
        assertThat(shown).isTrue()
        assertThat(hidden).isFalse()
    }

    @Test
    fun `GIVEN model created WHEN back clicked THEN router pops`() = runTest {
        // Arrange
        val model = createModel()
        advanceUntilIdle()

        // Act
        model.uiState.value.onBackClick()

        // Assert
        verify(exactly = 1) { router.pop() }
    }

    private fun TestScope.createModel(action: Action = Action.Activate) = PolymarketFastTradingActivationModel(
        dispatchers = createTestingCoroutineDispatcherProvider(),
        paramsContainer = MutableParamsContainer(
            PolymarketFastTradingActivationComponent.Params(userWalletId = USER_WALLET_ID, action = action),
        ),
        router = router,
        startOperation = startOperation,
        messageSender = messageSender,
        getUserWalletUseCase = getUserWalletUseCase,
        observeOperationState = observeOperationState,
    ).also { model = it }

    private fun state(
        running: PolymarketSessionKeyOperation? = null,
        finished: PolymarketSessionKeyOperationOutcome? = null,
        lastOutcome: PolymarketSessionKeyOperationOutcome? = null,
    ) = PolymarketSessionKeyOperationState(running = running, finished = finished, lastOutcome = lastOutcome)

    private fun operation(stage: PolymarketSessionKeyOperationStage, id: Long = OPERATION_ID) =
        PolymarketSessionKeyOperation(id = id, request = ACTIVATE, targetAddress = null, stage = stage)

    private fun outcome(
        id: Long,
        result: arrow.core.Either<PolymarketSessionKeyOperationFailure, PolymarketSessionKey>,
    ) = PolymarketSessionKeyOperationOutcome(id = id, request = ACTIVATE, result = result)

    private companion object {

        @JvmStatic
        fun actions() = listOf(
            Arguments.of(Action.Activate, ACTIVATE, R.string.common_activate),
            Arguments.of(
                Action.Reactivate(PolymarketSessionKeyOperationRequest.Reactivate),
                PolymarketSessionKeyOperationRequest.Reactivate,
                R.string.prediction_session_key_reactivate,
            ),
            Arguments.of(
                Action.Reactivate(ACTIVATE),
                ACTIVATE,
                R.string.prediction_session_key_reactivate,
            ),
            Arguments.of(
                Action.Extend,
                PolymarketSessionKeyOperationRequest.Extend,
                R.string.prediction_session_key_banner_extend_button,
            ),
        )

        val USER_WALLET_ID = UserWalletId("011")
        const val OPERATION_ID = 5L
        val ACTIVATE = PolymarketSessionKeyOperationRequest.Activate
        val CANCELLED = PolymarketSessionKeyOperationFailure(
            error = PolymarketSessionKeyLifecycleError.Failed(
                PolymarketSessionKeyError.SigningFailed(PolymarketSigningError.UserCancelled),
            ),
            isAfterRevocation = false,
        )
        val SESSION_KEY = PolymarketSessionKey(
            address = "0x5e55000000000000000000000000000000000001",
            deviceId = "this-device",
            deviceName = "Pixel 9",
            isEnabled = true,
            status = PolymarketSessionKeyStatus.ACTIVE,
            scopes = listOf("CLOB"),
            validUntil = null,
            renewAfter = null,
            authorizedAt = null,
            createdAt = Instant.EPOCH,
        )
    }
}