package com.tangem.features.tangempay.deeplink

import android.net.Uri
import com.tangem.common.routing.AppRoute
import com.tangem.core.decompose.navigation.Router
import com.tangem.core.navigation.deeplink.DeepLinkRequest
import io.mockk.every
import io.mockk.mockk
import io.mockk.verify
import kotlinx.coroutines.test.TestScope
import org.junit.jupiter.api.Test

internal class DefaultTangemPayOnboardDeepLinkHandlerTest {

    private val router: Router = mockk(relaxed = true)

    @Test
    fun `GIVEN money account link WHEN handled THEN Tangem Pay onboarding opens with that link`() {
        // Arrange
        val link = "tangem://onboard-virtual-account?entry=ff1&id=VRTACC01"
        val uri = mockk<Uri> { every { this@mockk.toString() } returns link }

        // Act
        DefaultTangemPayOnboardDeepLinkHandler().handle(
            DeepLinkRequest(
                uri = uri,
                queryParams = emptyMap(),
                scope = TestScope(),
                isFromOnNewIntent = false,
                router = router,
            ),
        )

        // Assert
        verify(exactly = 1) {
            router.push(
                route = AppRoute.TangemPayOnboarding(AppRoute.TangemPayOnboarding.Mode.Deeplink(deeplink = link)),
                onComplete = any(),
            )
        }
    }
}