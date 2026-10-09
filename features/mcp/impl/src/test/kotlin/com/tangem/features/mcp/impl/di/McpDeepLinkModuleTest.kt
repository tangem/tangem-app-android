package com.tangem.features.mcp.impl.di

import android.net.Uri
import com.google.common.truth.Truth.assertThat
import com.tangem.common.routing.DeepLinkRoute
import com.tangem.common.routing.deeplink.DeepLinkRequest
import com.tangem.features.mcp.api.McpFeatureToggles
import com.tangem.features.mcp.api.deeplink.McpIntentDeepLinkHandler
import com.tangem.features.mcp.impl.deeplink.DefaultMcpIntentDeepLinkHandler
import io.mockk.clearMocks
import io.mockk.every
import io.mockk.mockk
import io.mockk.verify
import kotlinx.coroutines.test.TestScope
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Test

internal class McpDeepLinkModuleTest {

    private val factory: McpIntentDeepLinkHandler.Factory = mockk(relaxed = true)
    private val mcpFeatureToggles: McpFeatureToggles = mockk()

    private val handler = McpDeepLinkModule.provideIntentDeepLinkHandler(factory, mcpFeatureToggles)

    @BeforeEach
    fun resetMocks() {
        clearMocks(factory, mcpFeatureToggles)
    }

    @Test
    fun `WHEN intent handler is registered THEN it is keyed by the intent deep link host`() {
        // Assert
        assertThat(DefaultMcpIntentDeepLinkHandler.DEEPLINK_KEY).isEqualTo("tangem://${DeepLinkRoute.Intent.host}")
    }

    @Test
    fun `GIVEN mcp toggle on WHEN isEnabled THEN handler is enabled`() {
        // Arrange
        every { mcpFeatureToggles.isMcpEnabled } returns true

        // Assert
        assertThat(handler.isEnabled).isTrue()
    }

    @Test
    fun `GIVEN mcp toggle off WHEN isEnabled THEN handler is disabled`() {
        // Arrange
        every { mcpFeatureToggles.isMcpEnabled } returns false

        // Assert
        assertThat(handler.isEnabled).isFalse()
    }

    @Test
    fun `GIVEN deep link request WHEN handle THEN query params go to the intent handler factory`() {
        // Arrange
        val queryParams = mapOf("id" to "my_intent_id123", "source" to "express", "type" to "exchange")
        val request = DeepLinkRequest(
            uri = mockk<Uri>(),
            queryParams = queryParams,
            scope = TestScope(),
            isFromOnNewIntent = false,
        )

        // Act
        handler.handle(request)

        // Assert
        verify(exactly = 1) { factory.create(queryParams) }
    }
}