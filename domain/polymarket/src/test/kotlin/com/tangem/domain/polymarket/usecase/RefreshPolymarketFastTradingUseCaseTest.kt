package com.tangem.domain.polymarket.usecase

import com.tangem.domain.models.wallet.UserWalletId
import com.tangem.domain.polymarket.sessionkey.PolymarketSessionKeyStateService
import io.mockk.coEvery
import io.mockk.coVerify
import io.mockk.mockk
import kotlinx.coroutines.test.runTest
import org.junit.jupiter.api.Test

internal class RefreshPolymarketFastTradingUseCaseTest {

    private val stateService: PolymarketSessionKeyStateService = mockk()
    private val useCase = RefreshPolymarketFastTradingUseCase(stateService)

    @Test
    fun `GIVEN a wallet WHEN invoke THEN the session key state is refreshed and awaited`() = runTest {
        // Arrange
        coEvery { stateService.refreshAndAwait(USER_WALLET_ID, any()) } returns Unit

        // Act
        useCase(USER_WALLET_ID)

        // Assert
        coVerify(exactly = 1) { stateService.refreshAndAwait(USER_WALLET_ID, any()) }
    }

    private companion object {
        val USER_WALLET_ID = UserWalletId("011")
    }
}