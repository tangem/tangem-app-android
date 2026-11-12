package com.tangem.features.polymarket.impl.fasttrading.model

import arrow.core.left
import arrow.core.right
import com.google.common.truth.Truth.assertThat
import com.tangem.core.decompose.model.MutableParamsContainer
import com.tangem.core.decompose.navigation.Router
import com.tangem.core.decompose.ui.UiMessageSender
import com.tangem.core.ui.message.EventMessage
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
import com.tangem.domain.polymarket.usecase.ObservePolymarketSessionKeyOperationUseCase
import com.tangem.domain.polymarket.usecase.ObservePolymarketSessionKeyOutcomeUseCase
import com.tangem.domain.polymarket.usecase.StartPolymarketSessionKeyOperationUseCase
import com.tangem.features.polymarket.impl.fasttrading.PolymarketFastTradingActivationComponent
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
import java.time.Instant

@OptIn(ExperimentalCoroutinesApi::class)
internal class PolymarketFastTradingActivationModelTest {

    private val router: Router = mockk(relaxed = true)
    private val startOperation: StartPolymarketSessionKeyOperationUseCase = mockk()
    private val messageSender: UiMessageSender = mockk()
    private val observeOperation: ObservePolymarketSessionKeyOperationUseCase = mockk()
    private val observeOutcome: ObservePolymarketSessionKeyOutcomeUseCase = mockk()
    private val running = MutableStateFlow<PolymarketSessionKeyOperation?>(null)
    private val finished = MutableStateFlow<PolymarketSessionKeyOperationOutcome?>(null)
    private val sentMessages = mutableListOf<EventMessage>()
    private var model: PolymarketFastTradingActivationModel? = null

    @BeforeEach
    fun setup() {
        clearMocks(router, startOperation, messageSender, observeOperation, observeOutcome)
        running.value = null
        finished.value = null
        sentMessages.clear()
        every { observeOperation(USER_WALLET_ID) } returns running
        every { observeOutcome(USER_WALLET_ID) } returns finished
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
        running.value = operation(PolymarketSessionKeyOperationStage.SIGNING)
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
        running.value = operation(PolymarketSessionKeyOperationStage.SIGNING)
        advanceUntilIdle()

        // Act
        running.value = operation(PolymarketSessionKeyOperationStage.AUTHORIZING)
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
        finished.value = outcome(OPERATION_ID, SESSION_KEY.right())
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
        running.value = operation(PolymarketSessionKeyOperationStage.SIGNING)
        advanceUntilIdle()

        // Act
        running.value = null
        finished.value = outcome(OPERATION_ID, CANCELLED.left())
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
            running.value = operation(PolymarketSessionKeyOperationStage.SIGNING)
            advanceUntilIdle()

            // Act
            running.value = null
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
            running.value = operation(PolymarketSessionKeyOperationStage.SIGNING)
            advanceUntilIdle()

            // Act
            running.value = operation(PolymarketSessionKeyOperationStage.SIGNING, id = OPERATION_ID + 1)
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
            running.value = operation(PolymarketSessionKeyOperationStage.SIGNING, id = OPERATION_ID + 1)
            finished.value = outcome(OPERATION_ID + 1, CANCELLED.left())
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

    private fun TestScope.createModel() = PolymarketFastTradingActivationModel(
        dispatchers = createTestingCoroutineDispatcherProvider(),
        paramsContainer = MutableParamsContainer(PolymarketFastTradingActivationComponent.Params(USER_WALLET_ID)),
        router = router,
        startOperation = startOperation,
        messageSender = messageSender,
        observeOperation = observeOperation,
        observeOutcome = observeOutcome,
    ).also { model = it }

    private fun operation(stage: PolymarketSessionKeyOperationStage, id: Long = OPERATION_ID) =
        PolymarketSessionKeyOperation(id = id, request = ACTIVATE, targetAddress = null, stage = stage)

    private fun outcome(
        id: Long,
        result: arrow.core.Either<PolymarketSessionKeyOperationFailure, PolymarketSessionKey>,
    ) = PolymarketSessionKeyOperationOutcome(id = id, request = ACTIVATE, result = result)

    private companion object {
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