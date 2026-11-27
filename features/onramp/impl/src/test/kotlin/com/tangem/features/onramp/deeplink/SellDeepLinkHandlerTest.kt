package com.tangem.features.onramp.deeplink

import android.net.Uri
import arrow.core.left
import arrow.core.right
import com.tangem.common.routing.AppRoute
import com.tangem.common.test.domain.wallet.MockUserWalletFactory
import com.tangem.core.decompose.navigation.Router
import com.tangem.core.navigation.deeplink.DeepLinkRequest
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

    private val router: Router = mockk(relaxed = true)
    private val getSelectedWalletSyncUseCase: GetSelectedWalletSyncUseCase = mockk()

    private val handler = SellDeepLinkHandler(
        getSelectedWalletSyncUseCase = getSelectedWalletSyncUseCase,
    )

    @BeforeEach
    fun resetMocks() {
        clearMocks(router, getSelectedWalletSyncUseCase)
    }

    @Test
    fun `GIVEN selected wallet WHEN handled THEN sell screen opens for that wallet`() {
        // Arrange
        val userWallet = MockUserWalletFactory.create()
        every { getSelectedWalletSyncUseCase() } returns userWallet.right()

        // Act
        handler.handle(createRequest())

        // Assert
        verify(exactly = 1) { router.push(AppRoute.SellCrypto(userWallet.walletId), any()) }
    }

    @Test
    fun `GIVEN no selected wallet WHEN handled THEN nothing opens`() {
        // Arrange
        every { getSelectedWalletSyncUseCase() } returns GetUserWalletError.UserWalletNotFound.left()

        // Act
        handler.handle(createRequest())

        // Assert
        verify(exactly = 0) { router.push(any(), any()) }
    }

    private fun createRequest() = DeepLinkRequest(
        uri = mockk<Uri>(),
        queryParams = emptyMap(),
        scope = mockk<CoroutineScope>(),
        isFromOnNewIntent = false,
        router = router,
    )
}