package com.tangem.features.polymarket.impl.model

import arrow.core.left
import com.google.common.truth.Truth.assertThat
import com.tangem.core.decompose.ui.UiMessageSender
import com.tangem.core.res.R
import com.tangem.core.ui.extensions.resourceReference
import com.tangem.core.ui.message.SnackbarMessage
import com.tangem.domain.models.wallet.UserWalletId
import com.tangem.domain.polymarket.model.PolymarketSessionKeyError
import com.tangem.domain.polymarket.model.PolymarketSessionKeyLifecycleError
import com.tangem.domain.polymarket.model.PolymarketSigningError
import com.tangem.domain.polymarket.sessionkey.operation.PolymarketSessionKeyOperation
import com.tangem.domain.polymarket.sessionkey.operation.PolymarketSessionKeyOperationFailure
import com.tangem.domain.polymarket.sessionkey.operation.PolymarketSessionKeyOperationOutcome
import com.tangem.domain.polymarket.sessionkey.operation.PolymarketSessionKeyOperationRequest
import com.tangem.domain.polymarket.sessionkey.operation.PolymarketSessionKeyOperationStage
import com.tangem.domain.polymarket.usecase.AcknowledgePolymarketSessionKeyOutcomeUseCase
import com.tangem.domain.polymarket.usecase.ObservePolymarketSessionKeyOperationUseCase
import com.tangem.domain.polymarket.usecase.ObservePolymarketSessionKeyOutcomeUseCase
import com.tangem.features.polymarket.api.PolymarketFeatureToggles
import com.tangem.test.core.createTestingCoroutineDispatcherProvider
import io.mockk.clearMocks
import io.mockk.every
import io.mockk.mockk
import io.mockk.slot
import io.mockk.verify
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.test.TestScope
import kotlinx.coroutines.test.advanceUntilIdle
import kotlinx.coroutines.test.runTest
import org.junit.jupiter.api.AfterEach
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.TestInstance
import org.junit.jupiter.api.Test

@OptIn(ExperimentalCoroutinesApi::class)
@TestInstance(TestInstance.Lifecycle.PER_CLASS)
internal class PolymarketModelTest {

    private val featureToggles: PolymarketFeatureToggles = mockk()
    private val observeOutcome: ObservePolymarketSessionKeyOutcomeUseCase = mockk()
    private val acknowledgeOutcome: AcknowledgePolymarketSessionKeyOutcomeUseCase = mockk()
    private val observeOperation: ObservePolymarketSessionKeyOperationUseCase = mockk()
    private val messageSender: UiMessageSender = mockk(relaxed = true)
    private val outcomes = mapOf(
        USER_WALLET_ID to MutableStateFlow<PolymarketSessionKeyOperationOutcome?>(null),
        OTHER_WALLET_ID to MutableStateFlow<PolymarketSessionKeyOperationOutcome?>(null),
    )
    private val operations = mapOf(
        USER_WALLET_ID to MutableStateFlow<PolymarketSessionKeyOperation?>(null),
        OTHER_WALLET_ID to MutableStateFlow<PolymarketSessionKeyOperation?>(null),
    )
    private var model: PolymarketModel? = null

    @BeforeEach
    fun setup() {
        clearMocks(featureToggles, observeOutcome, acknowledgeOutcome, observeOperation, messageSender)
        every { featureToggles.isPolymarketSessionKeysEnabled } returns true
        outcomes.forEach { (walletId, finished) ->
            finished.value = null
            every { observeOutcome(walletId) } returns finished
            every { acknowledgeOutcome(walletId, any()) } answers {
                finished.value = finished.value?.takeIf { it.id != secondArg<Long>() }
            }
        }
        operations.forEach { (walletId, running) ->
            running.value = null
            every { observeOperation(walletId) } returns running
        }
    }

    @AfterEach
    fun destroyModel() {
        model?.onDestroy()
    }

    @Test
    fun `GIVEN visible host WHEN failed outcome arrives THEN toast is sent and outcome acknowledged`() =
        runTest {
            // Arrange
            createVisibleModel(USER_WALLET_ID)
            val message = slot<SnackbarMessage>()
            every { messageSender.send(capture(message)) } returns Unit

            // Act
            finish(USER_WALLET_ID, REVOKE_FAILED)

            // Assert
            verify(exactly = 1) { messageSender.send(any()) }
            assertThat(message.captured.message)
                .isEqualTo(resourceReference(R.string.prediction_session_key_toast_revocation_failed))
            verify(exactly = 1) { acknowledgeOutcome(USER_WALLET_ID, REVOKE_FAILED.id) }
        }

    @Test
    fun `GIVEN outcome without toast WHEN it arrives THEN nothing is sent and outcome acknowledged`() = runTest {
        // Arrange
        createVisibleModel(USER_WALLET_ID)

        // Act
        finish(USER_WALLET_ID, ACTIVATE_CANCELLED)

        // Assert
        verify(exactly = 0) { messageSender.send(any()) }
        verify(exactly = 1) { acknowledgeOutcome(USER_WALLET_ID, ACTIVATE_CANCELLED.id) }
    }

    @Test
    fun `GIVEN outcome finished before wallet is active WHEN wallet becomes active THEN toast is sent once`() =
        runTest {
            // Arrange
            finish(USER_WALLET_ID, REVOKE_FAILED)
            val model = createModel()
            model.onVisibilityChanged(isVisible = true)
            advanceUntilIdle()

            // Act
            model.onActiveWalletChanged(USER_WALLET_ID)
            advanceUntilIdle()

            // Assert
            verify(exactly = 1) { messageSender.send(any()) }
            verify(exactly = 1) { acknowledgeOutcome(USER_WALLET_ID, REVOKE_FAILED.id) }
        }

    @Test
    fun `GIVEN host not visible WHEN outcome arrives THEN nothing is sent or acknowledged`() = runTest {
        // Arrange
        val model = createModel()
        model.onActiveWalletChanged(USER_WALLET_ID)
        advanceUntilIdle()

        // Act
        finish(USER_WALLET_ID, REVOKE_FAILED)

        // Assert
        verify(exactly = 0) { messageSender.send(any()) }
        verify(exactly = 0) { acknowledgeOutcome(any(), any()) }
    }

    @Test
    fun `GIVEN outcome waiting while host not visible WHEN host becomes visible THEN toast is sent once`() =
        runTest {
            // Arrange
            val model = createModel()
            model.onActiveWalletChanged(USER_WALLET_ID)
            finish(USER_WALLET_ID, REVOKE_FAILED)

            // Act
            model.onVisibilityChanged(isVisible = true)
            advanceUntilIdle()

            // Assert
            verify(exactly = 1) { messageSender.send(any()) }
            verify(exactly = 1) { acknowledgeOutcome(USER_WALLET_ID, REVOKE_FAILED.id) }
        }

    @Test
    fun `GIVEN visible host WHEN it is stopped and outcome arrives THEN nothing is sent or acknowledged`() =
        runTest {
            // Arrange
            val model = createVisibleModel(USER_WALLET_ID)

            // Act
            model.onVisibilityChanged(isVisible = false)
            advanceUntilIdle()
            finish(USER_WALLET_ID, REVOKE_FAILED)

            // Assert
            verify(exactly = 0) { messageSender.send(any()) }
            verify(exactly = 0) { acknowledgeOutcome(any(), any()) }
        }

    @Test
    fun `GIVEN active wallet WHEN stack returns to a route without wallet THEN later outcomes are not toasted`() =
        runTest {
            // Arrange
            val model = createVisibleModel(USER_WALLET_ID)

            // Act
            model.onActiveWalletChanged(null)
            advanceUntilIdle()
            finish(USER_WALLET_ID, REVOKE_FAILED)

            // Assert
            verify(exactly = 0) { messageSender.send(any()) }
            verify(exactly = 0) { acknowledgeOutcome(any(), any()) }
        }

    @Test
    fun `GIVEN active wallet WHEN another wallet becomes active THEN only the new wallet outcomes are toasted`() =
        runTest {
            // Arrange
            val model = createVisibleModel(USER_WALLET_ID)

            // Act
            model.onActiveWalletChanged(OTHER_WALLET_ID)
            advanceUntilIdle()
            finish(USER_WALLET_ID, REVOKE_FAILED)
            finish(OTHER_WALLET_ID, OTHER_REVOKE_FAILED)

            // Assert
            verify(exactly = 1) { messageSender.send(any()) }
            verify(exactly = 0) { acknowledgeOutcome(USER_WALLET_ID, any()) }
            verify(exactly = 1) { acknowledgeOutcome(OTHER_WALLET_ID, OTHER_REVOKE_FAILED.id) }
        }

    @Test
    fun `GIVEN toggle off WHEN outcome arrives THEN nothing is observed sent or acknowledged`() = runTest {
        // Arrange
        every { featureToggles.isPolymarketSessionKeysEnabled } returns false
        createVisibleModel(USER_WALLET_ID)

        // Act
        finish(USER_WALLET_ID, REVOKE_FAILED)

        // Assert
        verify(exactly = 0) { observeOutcome(any()) }
        verify(exactly = 0) { observeOperation(any()) }
        verify(exactly = 0) { messageSender.send(any()) }
        verify(exactly = 0) { acknowledgeOutcome(any(), any()) }
    }

    @Test
    fun `GIVEN no active wallet WHEN host becomes visible THEN outcomes are not observed`() = runTest {
        // Arrange
        val model = createModel()

        // Act
        model.onVisibilityChanged(isVisible = true)
        advanceUntilIdle()

        // Assert
        verify(exactly = 0) { observeOutcome(any()) }
    }

    @Test
    fun `GIVEN visible host WHEN revocation leaves signing THEN revoking toast is sent`() = runTest {
        // Arrange
        createVisibleModel(USER_WALLET_ID)
        val message = slot<SnackbarMessage>()
        every { messageSender.send(capture(message)) } returns Unit

        // Act
        run(USER_WALLET_ID, revocation(PolymarketSessionKeyOperationStage.SIGNING))
        run(USER_WALLET_ID, revocation(PolymarketSessionKeyOperationStage.REVOKING))

        // Assert
        verify(exactly = 1) { messageSender.send(any()) }
        assertThat(message.captured.message)
            .isEqualTo(resourceReference(R.string.prediction_session_key_toast_revoking))
    }

    @Test
    fun `GIVEN visible host WHEN revocation ends at signing THEN nothing is sent`() = runTest {
        // Arrange
        createVisibleModel(USER_WALLET_ID)

        // Act
        run(USER_WALLET_ID, revocation(PolymarketSessionKeyOperationStage.SIGNING))
        run(USER_WALLET_ID, null)

        // Assert
        verify(exactly = 0) { messageSender.send(any()) }
    }

    @Test
    fun `GIVEN visible host WHEN revocation passes several stages after signing THEN revoking toast is sent once`() =
        runTest {
            // Arrange
            createVisibleModel(USER_WALLET_ID)

            // Act
            run(USER_WALLET_ID, revocation(PolymarketSessionKeyOperationStage.SIGNING))
            run(USER_WALLET_ID, revocation(PolymarketSessionKeyOperationStage.REVOKING))
            run(USER_WALLET_ID, revocation(PolymarketSessionKeyOperationStage.REVOKED))

            // Assert
            verify(exactly = 1) { messageSender.send(any()) }
        }

    @Test
    fun `GIVEN revocation already revoked WHEN host first sees it THEN nothing is sent`() = runTest {
        // Arrange
        createVisibleModel(USER_WALLET_ID)

        // Act
        run(USER_WALLET_ID, revocation(PolymarketSessionKeyOperationStage.REVOKED))

        // Assert
        verify(exactly = 0) { messageSender.send(any()) }
    }

    @Test
    fun `GIVEN visible host WHEN another operation leaves signing THEN nothing is sent`() = runTest {
        // Arrange
        createVisibleModel(USER_WALLET_ID)

        // Act
        run(USER_WALLET_ID, EXTEND.copy(stage = PolymarketSessionKeyOperationStage.SIGNING))
        run(USER_WALLET_ID, EXTEND.copy(stage = PolymarketSessionKeyOperationStage.REVOKING))
        run(USER_WALLET_ID, EXTEND.copy(stage = PolymarketSessionKeyOperationStage.AUTHORIZING))

        // Assert
        verify(exactly = 0) { messageSender.send(any()) }
    }

    @Test
    fun `GIVEN two revocations WHEN each leaves signing THEN each is toasted`() = runTest {
        // Arrange
        createVisibleModel(USER_WALLET_ID)

        // Act
        run(USER_WALLET_ID, revocation(PolymarketSessionKeyOperationStage.REVOKING))
        run(USER_WALLET_ID, null)
        run(USER_WALLET_ID, revocation(PolymarketSessionKeyOperationStage.REVOKING).copy(id = 9L))

        // Assert
        verify(exactly = 2) { messageSender.send(any()) }
    }

    private fun TestScope.run(walletId: UserWalletId, operation: PolymarketSessionKeyOperation?) {
        operations.getValue(walletId).value = operation
        advanceUntilIdle()
    }

    private fun TestScope.finish(walletId: UserWalletId, outcome: PolymarketSessionKeyOperationOutcome) {
        outcomes.getValue(walletId).value = outcome
        advanceUntilIdle()
    }

    private fun TestScope.createVisibleModel(walletId: UserWalletId): PolymarketModel {
        val model = createModel()
        model.onVisibilityChanged(isVisible = true)
        model.onActiveWalletChanged(walletId)
        advanceUntilIdle()
        return model
    }

    private fun TestScope.createModel() = PolymarketModel(
        featureToggles = featureToggles,
        observeSessionKeyOutcome = observeOutcome,
        acknowledgeSessionKeyOutcome = acknowledgeOutcome,
        observeSessionKeyOperation = observeOperation,
        messageSender = messageSender,
        dispatchers = createTestingCoroutineDispatcherProvider(),
    ).also { model = it }

    private companion object {
        val USER_WALLET_ID = UserWalletId("011")
        val OTHER_WALLET_ID = UserWalletId("022")
        val REVOKE_FAILED = revokeFailed(id = 5L)
        val OTHER_REVOKE_FAILED = revokeFailed(id = 7L)
        val ACTIVATE_CANCELLED = PolymarketSessionKeyOperationOutcome(
            id = 6L,
            request = PolymarketSessionKeyOperationRequest.Activate,
            result = PolymarketSessionKeyOperationFailure(
                error = PolymarketSessionKeyLifecycleError.Failed(
                    PolymarketSessionKeyError.SigningFailed(PolymarketSigningError.UserCancelled),
                ),
                isAfterRevocation = false,
            ).left(),
        )

        val EXTEND = PolymarketSessionKeyOperation(
            id = 8L,
            request = PolymarketSessionKeyOperationRequest.Extend,
            targetAddress = "0x1",
            stage = PolymarketSessionKeyOperationStage.SIGNING,
        )

        fun revocation(stage: PolymarketSessionKeyOperationStage) = PolymarketSessionKeyOperation(
            id = 4L,
            request = PolymarketSessionKeyOperationRequest.Revoke(sessionKeyAddress = "0x1"),
            targetAddress = "0x1",
            stage = stage,
        )

        fun revokeFailed(id: Long) = PolymarketSessionKeyOperationOutcome(
            id = id,
            request = PolymarketSessionKeyOperationRequest.Revoke(sessionKeyAddress = "0x1"),
            result = PolymarketSessionKeyOperationFailure(
                error = PolymarketSessionKeyLifecycleError.RevocationFailed(sessionKey = mockk()),
                isAfterRevocation = false,
            ).left(),
        )
    }
}