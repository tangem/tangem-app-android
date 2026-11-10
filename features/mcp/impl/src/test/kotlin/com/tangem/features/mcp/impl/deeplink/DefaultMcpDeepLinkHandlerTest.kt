package com.tangem.features.mcp.impl.deeplink

import android.net.Uri
import com.tangem.common.routing.deeplink.DeepLinkRequest
import com.tangem.domain.mcp.McpRequestService
import com.tangem.domain.mcp.models.McpRequest
import com.tangem.test.core.ProvideTestModels
import io.mockk.clearMocks
import io.mockk.mockk
import io.mockk.verify
import kotlinx.coroutines.CoroutineScope
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.TestInstance
import org.junit.jupiter.params.ParameterizedTest

@TestInstance(TestInstance.Lifecycle.PER_CLASS)
internal class DefaultMcpDeepLinkHandlerTest {

    private val mcpRequestService: McpRequestService = mockk(relaxed = true)

    private val handler = DefaultMcpDeepLinkHandler(
        mcpRequestService = mcpRequestService,
        mcpFeatureToggles = mockk(),
    )

    @BeforeEach
    fun resetMocks() {
        clearMocks(mcpRequestService)
    }

    @Test
    fun `GIVEN user code param WHEN handled THEN pair request is sent`() {
        // Act
        handler.handle(createRequest(queryParams = mapOf("user_code" to "WDJB-MJHT")))

        // Assert
        verify(exactly = 1) { mcpRequestService.request(McpRequest.Pair(userCode = "WDJB-MJHT")) }
    }

    @ParameterizedTest
    @ProvideTestModels
    fun `GIVEN no usable user code WHEN handled THEN no request is sent`(model: NoUserCodeModel) {
        // Act
        handler.handle(createRequest(queryParams = model.queryParams))

        // Assert
        verify(exactly = 0) { mcpRequestService.request(any()) }
    }

    data class NoUserCodeModel(val queryParams: Map<String, String>)

    private fun provideTestModels() = listOf(
        NoUserCodeModel(queryParams = mapOf("user_code" to "")),
        NoUserCodeModel(queryParams = mapOf("user_code" to " ")),
        NoUserCodeModel(queryParams = mapOf("foo" to "bar")),
    )

    private fun createRequest(queryParams: Map<String, String>) = DeepLinkRequest(
        uri = mockk<Uri>(),
        queryParams = queryParams,
        scope = mockk<CoroutineScope>(),
        isFromOnNewIntent = false,
    )
}