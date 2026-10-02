package com.tangem.features.mcp.impl.deeplink

import com.tangem.domain.mcp.McpRequestService
import com.tangem.domain.mcp.models.McpRequest
import io.mockk.clearMocks
import io.mockk.mockk
import io.mockk.verify
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Test
import org.junit.jupiter.params.ParameterizedTest
import org.junit.jupiter.params.provider.ValueSource

internal class DefaultMcpDeepLinkHandlerTest {

    private val mcpRequestService: McpRequestService = mockk(relaxed = true)

    @BeforeEach
    fun resetMocks() {
        clearMocks(mcpRequestService)
    }

    @Test
    fun `GIVEN user code param WHEN handler created THEN pair request is sent`() {
        // Act
        DefaultMcpDeepLinkHandler(
            queryParams = mapOf("user_code" to "WDJB-MJHT"),
            mcpRequestService = mcpRequestService,
        )

        // Assert
        verify(exactly = 1) { mcpRequestService.request(McpRequest.Pair(userCode = "WDJB-MJHT")) }
    }

    @ParameterizedTest
    @ValueSource(strings = ["", " "])
    fun `GIVEN blank user code WHEN handler created THEN no request is sent`(userCode: String) {
        // Act
        DefaultMcpDeepLinkHandler(
            queryParams = mapOf("user_code" to userCode),
            mcpRequestService = mcpRequestService,
        )

        // Assert
        verify(exactly = 0) { mcpRequestService.request(any()) }
    }

    @Test
    fun `GIVEN no user code param WHEN handler created THEN no request is sent`() {
        // Act
        DefaultMcpDeepLinkHandler(
            queryParams = mapOf("foo" to "bar"),
            mcpRequestService = mcpRequestService,
        )

        // Assert
        verify(exactly = 0) { mcpRequestService.request(any()) }
    }
}