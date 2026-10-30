package com.tangem.feature.usedesk.deeplink.di

import android.net.Uri
import com.google.common.truth.Truth.assertThat
import com.tangem.common.routing.deeplink.DeepLinkRequest
import com.tangem.feature.usedesk.api.UseDeskFeatureToggles
import com.tangem.feature.usedesk.api.deeplink.SupportDeepLinkHandler
import com.tangem.feature.usedesk.deeplink.DefaultSupportDeepLinkHandler
import io.mockk.clearMocks
import io.mockk.every
import io.mockk.mockk
import io.mockk.verify
import kotlinx.coroutines.test.TestScope
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Test
import org.junit.jupiter.params.ParameterizedTest
import org.junit.jupiter.params.provider.ValueSource

internal class SupportDeepLinkModuleTest {

    private val factory: SupportDeepLinkHandler.Factory = mockk(relaxed = true)
    private val useDeskFeatureToggles: UseDeskFeatureToggles = mockk()

    private val handler = SupportDeepLinkModule.provideSupportDeepLinkHandler(factory, useDeskFeatureToggles)

    @BeforeEach
    fun resetMocks() {
        clearMocks(factory, useDeskFeatureToggles)
    }

    @Test
    fun `GIVEN support handler WHEN registered THEN it is keyed by the support deep link`() {
        // Assert
        assertThat(DefaultSupportDeepLinkHandler.DEEPLINK_KEY).isEqualTo("tangem://support")
    }

    @ParameterizedTest
    @ValueSource(booleans = [true, false])
    fun `GIVEN support chat deep link toggle WHEN isEnabled read THEN it follows the toggle`(isToggleOn: Boolean) {
        // Arrange
        every { useDeskFeatureToggles.isSupportChatDeepLinkEnabled } returns isToggleOn

        // Act
        val actual = handler.isEnabled

        // Assert
        assertThat(actual).isEqualTo(isToggleOn)
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