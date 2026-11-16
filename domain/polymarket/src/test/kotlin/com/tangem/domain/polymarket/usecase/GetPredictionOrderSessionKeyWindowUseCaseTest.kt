package com.tangem.domain.polymarket.usecase

import com.google.common.truth.Truth.assertThat
import com.tangem.domain.models.wallet.UserWalletId
import com.tangem.domain.polymarket.sessionkey.PolymarketOrderSigningRouter
import com.tangem.domain.polymarket.sessionkey.PolymarketSessionKeyStateService
import com.tangem.domain.polymarket.sessionkey.PolymarketSessionKeyStore
import com.tangem.domain.polymarket.sessionkey.PolymarketSessionKeyTradingStore
import io.mockk.coEvery
import io.mockk.coVerify
import io.mockk.every
import io.mockk.mockk
import kotlinx.coroutines.test.runTest
import org.junit.jupiter.api.Test
import java.time.Duration

internal class GetPredictionOrderSessionKeyWindowUseCaseTest {

    private val router: PolymarketOrderSigningRouter = mockk()
    private val sessionKeyStore: PolymarketSessionKeyStore = mockk()
    private val stateService: PolymarketSessionKeyStateService = mockk()
    private val tradingStore: PolymarketSessionKeyTradingStore = mockk {
        every { isRefused(any(), any()) } returns false
    }
    private val useCase = GetPredictionOrderSessionKeyWindowUseCase(
        router = router,
        sessionKeyStore = sessionKeyStore,
        stateService = stateService,
        tradingStore = tradingStore,
    )

    @Test
    fun `GIVEN a tradable session key WHEN invoke THEN its window without a refresh`() = runTest {
        // Arrange
        coEvery { router.sessionKeySigningWindow(USER_WALLET_ID) } returns WINDOW

        // Act
        val actual = useCase(USER_WALLET_ID)

        // Assert
        assertThat(actual).isEqualTo(WINDOW)
        coVerify(exactly = 0) { stateService.refreshAndAwait(any(), any()) }
    }

    @Test
    fun `GIVEN no session key on the device WHEN invoke THEN no window without a refresh`() = runTest {
        // Arrange
        coEvery { router.sessionKeySigningWindow(USER_WALLET_ID) } returns null
        coEvery { sessionKeyStore.heldAddress(USER_WALLET_ID) } returns null

        // Act
        val actual = useCase(USER_WALLET_ID)

        // Assert
        assertThat(actual).isNull()
        coVerify(exactly = 0) { stateService.refreshAndAwait(any(), any()) }
    }

    @Test
    fun `GIVEN a held key not ready yet WHEN the refresh makes it ready THEN its window`() = runTest {
        // Arrange
        coEvery { router.sessionKeySigningWindow(USER_WALLET_ID) } returnsMany listOf(null, WINDOW)
        coEvery { sessionKeyStore.heldAddress(USER_WALLET_ID) } returns KEY
        coEvery { stateService.refreshAndAwait(USER_WALLET_ID, any()) } returns Unit

        // Act
        val actual = useCase(USER_WALLET_ID)

        // Assert
        assertThat(actual).isEqualTo(WINDOW)
        coVerify(exactly = 1) { stateService.refreshAndAwait(USER_WALLET_ID, any()) }
    }

    @Test
    fun `GIVEN a held key still not ready after the refresh WHEN invoke THEN no window`() = runTest {
        // Arrange
        coEvery { router.sessionKeySigningWindow(USER_WALLET_ID) } returns null
        coEvery { sessionKeyStore.heldAddress(USER_WALLET_ID) } returns KEY
        coEvery { stateService.refreshAndAwait(USER_WALLET_ID, any()) } returns Unit

        // Act
        val actual = useCase(USER_WALLET_ID)

        // Assert
        assertThat(actual).isNull()
        coVerify(exactly = 1) { stateService.refreshAndAwait(USER_WALLET_ID, any()) }
    }

    @Test
    fun `GIVEN a held key refused by Polymarket WHEN invoke THEN no window without a refresh`() = runTest {
        // Arrange
        coEvery { router.sessionKeySigningWindow(USER_WALLET_ID) } returns null
        coEvery { sessionKeyStore.heldAddress(USER_WALLET_ID) } returns KEY
        every { tradingStore.isRefused(USER_WALLET_ID, KEY) } returns true

        // Act
        val actual = useCase(USER_WALLET_ID)

        // Assert
        assertThat(actual).isNull()
        coVerify(exactly = 0) { stateService.refreshAndAwait(any(), any()) }
    }

    private companion object {
        val USER_WALLET_ID = UserWalletId("011")
        const val KEY = "0xsessionkey"
        val WINDOW: Duration = Duration.ofMinutes(3)
    }
}