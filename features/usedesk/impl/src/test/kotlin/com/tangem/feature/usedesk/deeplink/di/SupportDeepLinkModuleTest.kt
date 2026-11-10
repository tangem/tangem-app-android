package com.tangem.feature.usedesk.deeplink.di

import android.net.Uri
import com.google.common.truth.Truth.assertThat
import com.tangem.common.routing.deeplink.DeepLinkRequest
import com.tangem.feature.usedesk.api.deeplink.SupportDeepLinkHandler
import com.tangem.feature.usedesk.deeplink.DefaultSupportDeepLinkHandler
import io.mockk.clearMocks
import io.mockk.mockk
import io.mockk.verify
import kotlinx.coroutines.test.TestScope
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Test

internal class SupportDeepLinkModuleTest {

    private val factory: SupportDeepLinkHandler.Factory = mockk(relaxed = true)

    private val handler = SupportDeepLinkModule.provideSupportDeepLinkHandler(factory)

    @BeforeEach
    fun resetMocks() {
        clearMocks(factory)
    }

    @Test
    fun `GIVEN support handler WHEN registered THEN it is keyed by the support deep link`() {
        // Assert
        assertThat(DefaultSupportDeepLinkHandler.DEEPLINK_KEY).isEqualTo("tangem://support")
    }

    @Test
    fun `GIVEN support handler WHEN isEnabled read THEN it is enabled`() {
        // Act
        val actual = handler.isEnabled

        // Assert
        assertThat(actual).isTrue()
    }

    @Test
    fun `GIVEN deep link request WHEN handle THEN request scope goes to the support handler factory`() {
        // Arrange
        val scope = TestScope()
        val request = DeepLinkRequest(
            uri = mockk<Uri>(),
            queryParams = emptyMap(),
            scope = scope,
            isFromOnNewIntent = false,
        )

        // Act
        handler.handle(request)

        // Assert
        verify(exactly = 1) { factory.create(scope) }
    }
}