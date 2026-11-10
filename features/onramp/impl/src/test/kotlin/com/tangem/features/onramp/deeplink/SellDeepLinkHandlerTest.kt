package com.tangem.features.onramp.deeplink

import android.net.Uri
import arrow.core.left
import arrow.core.right
import com.tangem.common.routing.AppRoute
import com.tangem.common.routing.AppRouter
import com.tangem.common.routing.deeplink.DeepLinkRequest
import com.tangem.common.test.domain.wallet.MockUserWalletFactory
import com.tangem.domain.wallets.models.errors.GetUserWalletError
import com.tangem.domain.wallets.usecase.GetSelectedWalletSyncUseCase
import io.mockk.clearMocks
import io.mockk.every
import io.mockk.mockk
import io.mockk.verify
import kotlinx.coroutines.CoroutineScope
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Test

internal class SellDeepLinkHandlerTest {

    private val appRouter: AppRouter = mockk(relaxed = true)
    private val getSelectedWalletSyncUseCase: GetSelectedWalletSyncUseCase = mockk()

    private val handler = SellDeepLinkHandler(
        router = appRouter,
        getSelectedWalletSyncUseCase = getSelectedWalletSyncUseCase,
    )

    @BeforeEach
    fun resetMocks() {
        clearMocks(appRouter, getSelectedWalletSyncUseCase)
    }

    @Test
    fun `GIVEN selected wallet WHEN handled THEN sell screen opens for that wallet`() {
        // Arrange
        val userWallet = MockUserWalletFactory.create()
        every { getSelectedWalletSyncUseCase() } returns userWallet.right()

        // Act
        handler.handle(createRequest())

        // Assert
        verify(exactly = 1) { appRouter.push(AppRoute.SellCrypto(userWallet.walletId), any()) }
    }

    @Test
    fun `GIVEN no selected wallet WHEN handled THEN nothing opens`() {
        // Arrange
        every { getSelectedWalletSyncUseCase() } returns GetUserWalletError.UserWalletNotFound.left()

        // Act
        handler.handle(createRequest())

        // Assert
        verify(exactly = 0) { appRouter.push(any(), any()) }
    }

    private fun createRequest() = DeepLinkRequest(
        uri = mockk<Uri>(),
        queryParams = emptyMap(),
        scope = mockk<CoroutineScope>(),
        isFromOnNewIntent = false,
    )
}