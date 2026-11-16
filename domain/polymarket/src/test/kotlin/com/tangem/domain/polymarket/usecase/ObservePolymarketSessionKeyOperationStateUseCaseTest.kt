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

internal class ObservePolymarketSessionKeyOperationStateUseCaseTest {

    private val runner: PolymarketSessionKeyOperationRunner = mockk()
    private val useCase = ObservePolymarketSessionKeyOperationStateUseCase(runner)

    @Test
    fun `GIVEN operation finishes WHEN observed THEN running and finished arrive in one snapshot`() = runTest {
        // Arrange
        val states = MutableStateFlow(PolymarketSessionKeyOperationState(running = RUNNING, finished = null))
        every { runner.observe(USER_WALLET_ID) } returns states
        val emitted = getEmittedValues(useCase(USER_WALLET_ID))

        // Act
        states.value = PolymarketSessionKeyOperationState(running = null, finished = OUTCOME)
        advanceUntilIdle()

        // Assert
        assertThat(emitted).containsExactly(
            PolymarketSessionKeyOperationState(running = RUNNING, finished = null),
            PolymarketSessionKeyOperationState(running = null, finished = OUTCOME),
        ).inOrder()
    }

    private companion object {
        val USER_WALLET_ID = UserWalletId("011")
        val RUNNING = PolymarketSessionKeyOperation(
            id = 1L,
            request = PolymarketSessionKeyOperationRequest.Extend,
            targetAddress = null,
            stage = PolymarketSessionKeyOperationStage.SIGNING,
        )
        val OUTCOME = PolymarketSessionKeyOperationOutcome(
            id = 1L,
            request = PolymarketSessionKeyOperationRequest.Extend,
            result = PolymarketSessionKeyOperationFailure(
                error = PolymarketSessionKeyLifecycleError.OperationInProgress,
                isAfterRevocation = false,
            ).left(),
        )
    }
}