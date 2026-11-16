package com.tangem.feature.wallet.deeplink

import android.net.Uri
import com.tangem.common.routing.AppRoute
import com.tangem.common.routing.deeplink.DeepLinkRequest
import com.tangem.core.decompose.navigation.Router
import io.mockk.mockk
import io.mockk.verify
import kotlinx.coroutines.CoroutineScope
import org.junit.jupiter.api.Test

internal class WalletDeepLinkHandlerTest {

    private val router: Router = mockk(relaxed = true)

    private val handler = WalletDeepLinkHandler()

    @Test
    fun `GIVEN wallet deep link WHEN handled THEN app returns to the wallet screen`() {
        // Arrange
        val request = DeepLinkRequest(
            uri = mockk<Uri>(),
            queryParams = emptyMap(),
            scope = mockk<CoroutineScope>(),
            isFromOnNewIntent = false,
            router = router,
        )

        // Act
        handler.handle(request)

        // Assert
        verify(exactly = 1) { router.popTo(AppRoute.Wallet, any()) }
    }
}