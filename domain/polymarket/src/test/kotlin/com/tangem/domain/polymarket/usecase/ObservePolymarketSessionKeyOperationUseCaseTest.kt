package com.tangem.domain.polymarket.usecase

import arrow.core.left
import com.google.common.truth.Truth.assertThat
import com.tangem.domain.models.wallet.UserWalletId
import com.tangem.domain.polymarket.model.PolymarketSessionKeyLifecycleError
import com.tangem.domain.polymarket.sessionkey.operation.PolymarketSessionKeyOperation
import com.tangem.domain.polymarket.sessionkey.operation.PolymarketSessionKeyOperationFailure
import com.tangem.domain.polymarket.sessionkey.operation.PolymarketSessionKeyOperationOutcome
import com.tangem.domain.polymarket.sessionkey.operation.PolymarketSessionKeyOperationRequest
import com.tangem.domain.polymarket.sessionkey.operation.PolymarketSessionKeyOperationRunner
import com.tangem.domain.polymarket.sessionkey.operation.PolymarketSessionKeyOperationStage
import com.tangem.domain.polymarket.sessionkey.operation.PolymarketSessionKeyOperationState
import com.tangem.test.core.getEmittedValues
import io.mockk.every
import io.mockk.mockk
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.test.advanceUntilIdle
import kotlinx.coroutines.test.runTest
import org.junit.jupiter.api.Test

internal class ObservePolymarketSessionKeyOperationUseCaseTest {

    private val runner: PolymarketSessionKeyOperationRunner = mockk()
    private val useCase = ObservePolymarketSessionKeyOperationUseCase(runner)

    @Test
    fun `GIVEN operation state changes WHEN observed THEN only distinct running operations are emitted`() = runTest {
        // Arrange
        val states = MutableStateFlow(PolymarketSessionKeyOperationState.IDLE)
        every { runner.observe(USER_WALLET_ID) } returns states
        val emitted = getEmittedValues(useCase(USER_WALLET_ID))
        val revoking = RUNNING.copy(stage = PolymarketSessionKeyOperationStage.REVOKING)

        // Act
        states.value = PolymarketSessionKeyOperationState(running = RUNNING, finished = null)
        advanceUntilIdle()
        states.value = PolymarketSessionKeyOperationState(running = revoking, finished = null)
        advanceUntilIdle()
        states.value = PolymarketSessionKeyOperationState(running = null, finished = OUTCOME)
        advanceUntilIdle()
        states.value = PolymarketSessionKeyOperationState(running = null, finished = null)
        advanceUntilIdle()

        // Assert
        assertThat(emitted).containsExactly(null, RUNNING, revoking, null).inOrder()
    }

    private companion object {
        val USER_WALLET_ID = UserWalletId("011")
        val REVOKE = PolymarketSessionKeyOperationRequest.Revoke(sessionKeyAddress = "0x5e55")
        val RUNNING = PolymarketSessionKeyOperation(
            id = 1L,
            request = REVOKE,
            targetAddress = "0x5e55",
            stage = PolymarketSessionKeyOperationStage.SIGNING,
        )
        val OUTCOME = PolymarketSessionKeyOperationOutcome(
            id = 1L,
            request = REVOKE,
            result = PolymarketSessionKeyOperationFailure(
                error = PolymarketSessionKeyLifecycleError.OperationInProgress,
                isAfterRevocation = false,
            ).left(),
        )
    }
}