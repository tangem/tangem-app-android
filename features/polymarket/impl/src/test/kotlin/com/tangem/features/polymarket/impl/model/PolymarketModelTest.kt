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
import com.tangem.domain.polymarket.sessionkey.operation.PolymarketSessionKeyOperationFailure
import com.tangem.domain.polymarket.sessionkey.operation.PolymarketSessionKeyOperationOutcome
import com.tangem.domain.polymarket.sessionkey.operation.PolymarketSessionKeyOperationRequest
import com.tangem.domain.polymarket.usecase.AcknowledgePolymarketSessionKeyOutcomeUseCase
import com.tangem.domain.polymarket.usecase.ObservePolymarketSessionKeyOutcomeUseCase
import com.tangem.features.polymarket.api.PolymarketFeatureToggles
import com.tangem.test.core.createTestingCoroutineDispatcherProvider
import io.mockk.clearMocks
import io.mockk.every
import io.mockk.mockk
import io.mockk.slot
import io.mockk.verify
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.test.TestScope
import kotlinx.coroutines.test.advanceUntilIdle
import kotlinx.coroutines.test.runTest
import org.junit.jupiter.api.AfterEach
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Test

@OptIn(ExperimentalCoroutinesApi::class)
internal class PolymarketModelTest {

    private val featureToggles: PolymarketFeatureToggles = mockk()
    private val observeOutcome: ObservePolymarketSessionKeyOutcomeUseCase = mockk()
    private val acknowledgeOutcome: AcknowledgePolymarketSessionKeyOutcomeUseCase = mockk(relaxed = true)
    private val messageSender: UiMessageSender = mockk(relaxed = true)
    private val outcomes = MutableSharedFlow<PolymarketSessionKeyOperationOutcome?>()
    private var model: PolymarketModel? = null

    @BeforeEach
    fun setup() {
        clearMocks(featureToggles, observeOutcome, acknowledgeOutcome, messageSender)
        every { featureToggles.isPolymarketSessionKeysEnabled } returns true
        every { observeOutcome(USER_WALLET_ID) } returns outcomes
    }

    @AfterEach
    fun destroyModel() {
        model?.onDestroy()
    }

    @Test
    fun `GIVEN active wallet WHEN failed outcome arrives THEN toast is sent once and outcome acknowledged`() =
        runTest {
            // Arrange
            val model = createModel()
            model.onActiveWalletChanged(USER_WALLET_ID)
            advanceUntilIdle()
            val message = slot<SnackbarMessage>()
            every { messageSender.send(capture(message)) } returns Unit

            // Act
            outcomes.emit(REVOKE_FAILED)
            advanceUntilIdle()

            // Assert
            verify(exactly = 1) { messageSender.send(any()) }
            assertThat(message.captured.message)
                .isEqualTo(resourceReference(R.string.prediction_session_key_toast_revocation_failed))
            verify(exactly = 1) { acknowledgeOutcome(USER_WALLET_ID, REVOKE_FAILED.id) }
        }

    @Test
    fun `GIVEN outcome without toast WHEN it arrives THEN nothing is sent and outcome acknowledged`() = runTest {
        // Arrange
        val model = createModel()
        model.onActiveWalletChanged(USER_WALLET_ID)
        advanceUntilIdle()

        // Act
        outcomes.emit(ACTIVATE_CANCELLED)
        advanceUntilIdle()

        // Assert
        verify(exactly = 0) { messageSender.send(any()) }
        verify(exactly = 1) { acknowledgeOutcome(USER_WALLET_ID, ACTIVATE_CANCELLED.id) }
    }

    @Test
    fun `GIVEN active wallet WHEN stack returns to a route without wallet THEN later outcomes are not toasted`() =
        runTest {
            // Arrange
            val model = createModel()
            model.onActiveWalletChanged(USER_WALLET_ID)
            advanceUntilIdle()

            // Act
            model.onActiveWalletChanged(null)
            advanceUntilIdle()
            outcomes.emit(REVOKE_FAILED)
            advanceUntilIdle()

            // Assert
            verify(exactly = 0) { messageSender.send(any()) }
            verify(exactly = 0) { acknowledgeOutcome(any(), any()) }
        }

    @Test
    fun `GIVEN active wallet WHEN another wallet becomes active THEN outcomes of the new wallet are toasted`() =
        runTest {
            // Arrange
            val otherOutcomes = MutableSharedFlow<PolymarketSessionKeyOperationOutcome?>()
            every { observeOutcome(OTHER_WALLET_ID) } returns otherOutcomes
            val model = createModel()
            model.onActiveWalletChanged(USER_WALLET_ID)
            advanceUntilIdle()

            // Act
            model.onActiveWalletChanged(OTHER_WALLET_ID)
            advanceUntilIdle()
            outcomes.emit(REVOKE_FAILED)
            otherOutcomes.emit(ACTIVATE_CANCELLED)
            advanceUntilIdle()

            // Assert
            verify(exactly = 0) { acknowledgeOutcome(USER_WALLET_ID, any()) }
            verify(exactly = 1) { acknowledgeOutcome(OTHER_WALLET_ID, ACTIVATE_CANCELLED.id) }
        }

    @Test
    fun `GIVEN toggle off WHEN outcome arrives THEN nothing is observed sent or acknowledged`() = runTest {
        // Arrange
        every { featureToggles.isPolymarketSessionKeysEnabled } returns false
        val model = createModel()
        model.onActiveWalletChanged(USER_WALLET_ID)
        advanceUntilIdle()

        // Act
        outcomes.emit(REVOKE_FAILED)
        advanceUntilIdle()

        // Assert
        verify(exactly = 0) { observeOutcome(any()) }
        verify(exactly = 0) { messageSender.send(any()) }
        verify(exactly = 0) { acknowledgeOutcome(any(), any()) }
    }

    @Test
    fun `GIVEN no active wallet WHEN model is created THEN outcomes are not observed`() = runTest {
        // Act
        createModel()
        advanceUntilIdle()

        // Assert
        verify(exactly = 0) { observeOutcome(any()) }
    }

    private fun TestScope.createModel() = PolymarketModel(
        featureToggles = featureToggles,
        observeSessionKeyOutcome = observeOutcome,
        acknowledgeSessionKeyOutcome = acknowledgeOutcome,
        messageSender = messageSender,
        dispatchers = createTestingCoroutineDispatcherProvider(),
    ).also { model = it }

    private companion object {
        val USER_WALLET_ID = UserWalletId("011")
        val OTHER_WALLET_ID = UserWalletId("022")
        val REVOKE_FAILED = PolymarketSessionKeyOperationOutcome(
            id = 5L,
            request = PolymarketSessionKeyOperationRequest.Revoke(sessionKeyAddress = "0x1"),
            result = PolymarketSessionKeyOperationFailure(
                error = PolymarketSessionKeyLifecycleError.RevocationFailed(sessionKey = null),
                isAfterRevocation = false,
            ).left(),
        )
        val ACTIVATE_CANCELLED = PolymarketSessionKeyOperationOutcome(
            id = 6L,
            request = PolymarketSessionKeyOperationRequest.Activate,
            result = PolymarketSessionKeyOperationFailure(
                error = PolymarketSessionKeyLifecycleError.Failed(
                    PolymarketSessionKeyError.SigningFailed(
                        PolymarketSigningError.UserCancelled,
                    ),
                ),
                isAfterRevocation = false,
            ).left(),
        )
    }
}