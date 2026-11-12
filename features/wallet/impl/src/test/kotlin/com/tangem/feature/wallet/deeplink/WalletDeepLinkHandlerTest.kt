package com.tangem.feature.wallet.deeplink

import android.net.Uri
import com.tangem.common.routing.AppRoute
import com.tangem.common.routing.AppRouter
import com.tangem.common.routing.deeplink.DeepLinkRequest
import io.mockk.mockk
import io.mockk.verify
import kotlinx.coroutines.CoroutineScope
import org.junit.jupiter.api.Test

internal class WalletDeepLinkHandlerTest {

    private val appRouter: AppRouter = mockk(relaxed = true)

    private val handler = WalletDeepLinkHandler(router = appRouter)

    @Test
    fun `GIVEN wallet deep link WHEN handled THEN app returns to the wallet screen`() {
        // Arrange
        val request = DeepLinkRequest(
            uri = mockk<Uri>(),
            queryParams = emptyMap(),
            scope = mockk<CoroutineScope>(),
            isFromOnNewIntent = false,
        )

        // Act
        handler.handle(request)

        // Assert
        verify(exactly = 1) { appRouter.popTo(AppRoute.Wallet, any()) }
    }
}