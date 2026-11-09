package com.tangem.domain.polymarket.usecase

import com.google.common.truth.Truth.assertThat
import com.tangem.domain.models.wallet.UserWalletId
import com.tangem.domain.polymarket.sessionkey.operation.PolymarketSessionKeyOperationRequest
import com.tangem.domain.polymarket.sessionkey.operation.PolymarketSessionKeyOperationRunner
import com.tangem.domain.polymarket.sessionkey.operation.PolymarketSessionKeyOperationStart
import io.mockk.every
import io.mockk.mockk
import io.mockk.verify
import org.junit.jupiter.api.Test

internal class StartPolymarketSessionKeyOperationUseCaseTest {

    private val runner: PolymarketSessionKeyOperationRunner = mockk()
    private val useCase = StartPolymarketSessionKeyOperationUseCase(runner)

    @Test
    fun `GIVEN revoke request WHEN invoke THEN runner starts it and its start is returned`() {
        // Arrange
        val started = PolymarketSessionKeyOperationStart.Started(id = 7L)
        every { runner.start(USER_WALLET_ID, REVOKE) } returns started

        // Act
        val actual = useCase(USER_WALLET_ID, REVOKE)

        // Assert
        assertThat(actual).isEqualTo(started)
        verify(exactly = 1) { runner.start(USER_WALLET_ID, REVOKE) }
    }

    private companion object {
        val USER_WALLET_ID = UserWalletId("011")
        val REVOKE = PolymarketSessionKeyOperationRequest.Revoke(sessionKeyAddress = "0x5e55")
    }
}