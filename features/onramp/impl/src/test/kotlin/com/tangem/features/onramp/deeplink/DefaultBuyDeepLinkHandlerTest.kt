package com.tangem.features.onramp.deeplink

import arrow.core.Either
import com.tangem.common.routing.AppRoute
import com.tangem.common.routing.AppRouter
import com.tangem.domain.models.wallet.UserWallet
import com.tangem.domain.models.wallet.UserWalletId
import com.tangem.domain.wallets.models.errors.GetUserWalletError
import com.tangem.domain.wallets.usecase.GetSelectedWalletSyncUseCase
import com.tangem.utils.logging.TangemLogger
import io.mockk.*
import org.junit.jupiter.api.AfterEach
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Test
import org.junit.jupiter.params.ParameterizedTest
import org.junit.jupiter.params.provider.ValueSource

internal class DefaultBuyDeepLinkHandlerTest {

    private val router: AppRouter = mockk(relaxed = true)
    private val getSelectedWalletSyncUseCase: GetSelectedWalletSyncUseCase = mockk()

    @BeforeEach
    fun setUp() {
        clearMocks(router, getSelectedWalletSyncUseCase)
        mockkObject(TangemLogger)
        every { TangemLogger.e(any(), any(), any()) } just Runs
    }

    @AfterEach
    fun tearDown() {
        unmockkObject(TangemLogger)
    }

    @ParameterizedTest
    @ValueSource(strings = ["0011223344556677", "8899AABBCCDDEEFF"])
    fun `GIVEN selected wallet WHEN handler created THEN opens buy for that wallet exactly once`(walletId: String) {
        // Arrange
        val userWalletId = UserWalletId(walletId)
        val userWallet: UserWallet = mockk { every { this@mockk.walletId } returns userWalletId }
        every { getSelectedWalletSyncUseCase() } returns Either.Right(userWallet)

        // Act
        DefaultBuyDeepLinkHandler(router, getSelectedWalletSyncUseCase)

        // Assert
        verify(exactly = 1) { getSelectedWalletSyncUseCase() }
        verify(exactly = 1) { router.push(AppRoute.BuyCrypto(userWalletId), any()) }
        confirmVerified(router, getSelectedWalletSyncUseCase)
        verify(exactly = 0) { TangemLogger.e(any(), any(), any()) }
    }

    @Test
    fun `GIVEN no selected wallet WHEN handler created THEN logs error without navigation`() {
        // Arrange
        val error = GetUserWalletError.UserWalletNotFound
        every { getSelectedWalletSyncUseCase() } returns Either.Left(error)

        // Act
        DefaultBuyDeepLinkHandler(router, getSelectedWalletSyncUseCase)

        // Assert
        verify(exactly = 1) { getSelectedWalletSyncUseCase() }
        verify { router wasNot Called }
        verify(exactly = 1) { TangemLogger.e("Error on getting user wallet: $error", any(), any()) }
        confirmVerified(getSelectedWalletSyncUseCase)
    }
}