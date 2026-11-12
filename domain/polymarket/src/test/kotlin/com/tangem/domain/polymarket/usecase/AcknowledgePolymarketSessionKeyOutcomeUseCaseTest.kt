package com.tangem.domain.polymarket.usecase

import com.tangem.domain.models.wallet.UserWalletId
import com.tangem.domain.polymarket.sessionkey.operation.PolymarketSessionKeyOperationRunner
import io.mockk.every
import io.mockk.mockk
import io.mockk.verify
import org.junit.jupiter.api.Test

internal class AcknowledgePolymarketSessionKeyOutcomeUseCaseTest {

    private val runner: PolymarketSessionKeyOperationRunner = mockk()
    private val useCase = AcknowledgePolymarketSessionKeyOutcomeUseCase(runner)

    @Test
    fun `GIVEN finished operation WHEN invoke THEN runner acknowledges it`() {
        // Arrange
        every { runner.acknowledge(USER_WALLET_ID, 3L) } returns Unit

        // Act
        useCase(USER_WALLET_ID, 3L)

        // Assert
        verify(exactly = 1) { runner.acknowledge(USER_WALLET_ID, 3L) }
    }

    private companion object {
        val USER_WALLET_ID = UserWalletId("011")
    }
}