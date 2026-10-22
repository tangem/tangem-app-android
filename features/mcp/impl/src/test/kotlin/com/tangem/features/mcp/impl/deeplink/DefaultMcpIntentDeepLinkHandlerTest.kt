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

internal class DefaultMcpIntentDeepLinkHandlerTest {

    private val mcpRequestService: McpRequestService = mockk(relaxed = true)

    private val exchangeSource = McpRequest.Intent.Source.Express(McpRequest.Intent.Source.Express.Type.EXCHANGE)
    private val sendSource = McpRequest.Intent.Source.Transaction(McpRequest.Intent.Source.Transaction.Type.SEND)

    private val validParams = mapOf("id" to "my_intent_id123", "source" to "express", "type" to "exchange")

    @BeforeEach
    fun resetMocks() {
        clearMocks(mcpRequestService)
    }

    @Test
    fun `GIVEN express exchange link WHEN handler created THEN intent request is sent`() {
        // Act
        DefaultMcpIntentDeepLinkHandler(queryParams = validParams, mcpRequestService = mcpRequestService)

        // Assert
        verify(exactly = 1) {
            mcpRequestService.request(McpRequest.Intent(id = "my_intent_id123", source = exchangeSource))
        }
    }

    @ParameterizedTest
    @ValueSource(strings = ["", " "])
    fun `GIVEN express link without type WHEN handler created THEN it is taken as an exchange`(type: String) {
        // Arrange
        val params = validParams - "type" + ("type" to type)

        // Act
        DefaultMcpIntentDeepLinkHandler(queryParams = params, mcpRequestService = mcpRequestService)

        // Assert
        verify(exactly = 1) {
            mcpRequestService.request(McpRequest.Intent(id = "my_intent_id123", source = exchangeSource))
        }
    }

    @Test
    fun `GIVEN express link with no type param WHEN handler created THEN it is taken as an exchange`() {
        // Act
        DefaultMcpIntentDeepLinkHandler(queryParams = validParams - "type", mcpRequestService = mcpRequestService)

        // Assert
        verify(exactly = 1) {
            mcpRequestService.request(McpRequest.Intent(id = "my_intent_id123", source = exchangeSource))
        }
    }

    @ParameterizedTest
    @ValueSource(strings = ["id", "source"])
    fun `GIVEN missing param WHEN handler created THEN no request is sent`(missingKey: String) {
        // Act
        DefaultMcpIntentDeepLinkHandler(
            queryParams = validParams - missingKey,
            mcpRequestService = mcpRequestService,
        )

        // Assert
        verify(exactly = 0) { mcpRequestService.request(any()) }
    }

    @ParameterizedTest
    @ValueSource(strings = ["id", "source"])
    fun `GIVEN blank param WHEN handler created THEN no request is sent`(blankKey: String) {
        // Act
        DefaultMcpIntentDeepLinkHandler(
            queryParams = validParams + (blankKey to " "),
            mcpRequestService = mcpRequestService,
        )

        // Assert
        verify(exactly = 0) { mcpRequestService.request(any()) }
    }

    @Test
    fun `GIVEN transaction send link WHEN handler created THEN send intent request is sent`() {
        // Arrange
        val params = mapOf("id" to "my_intent_id123", "source" to "transaction", "type" to "send")

        // Act
        DefaultMcpIntentDeepLinkHandler(queryParams = params, mcpRequestService = mcpRequestService)

        // Assert
        verify(exactly = 1) {
            mcpRequestService.request(McpRequest.Intent(id = "my_intent_id123", source = sendSource))
        }
    }

    @ParameterizedTest
    @ValueSource(strings = ["receive", "SEND", "", " "])
    fun `GIVEN transaction link of an unsupported or blank type WHEN handler created THEN no request is sent`(
        type: String,
    ) {
        // Act
        DefaultMcpIntentDeepLinkHandler(
            queryParams = mapOf("id" to "my_intent_id123", "source" to "transaction", "type" to type),
            mcpRequestService = mcpRequestService,
        )

        // Assert
        verify(exactly = 0) { mcpRequestService.request(any()) }
    }

    @Test
    fun `GIVEN transaction link with no type param WHEN handler created THEN no request is sent`() {
        // Act
        DefaultMcpIntentDeepLinkHandler(
            queryParams = mapOf("id" to "my_intent_id123", "source" to "transaction"),
            mcpRequestService = mcpRequestService,
        )

        // Assert
        verify(exactly = 0) { mcpRequestService.request(any()) }
    }

    @ParameterizedTest
    @ValueSource(strings = ["onramp", "staking"])
    fun `GIVEN intent of an unknown service WHEN handler created THEN no request is sent`(source: String) {
        // Act
        DefaultMcpIntentDeepLinkHandler(
            queryParams = mapOf("id" to "my_intent_id123", "source" to source, "type" to "send"),
            mcpRequestService = mcpRequestService,
        )

        // Assert
        verify(exactly = 0) { mcpRequestService.request(any()) }
    }

    @Test
    fun `GIVEN express intent of an unsupported operation WHEN handler created THEN no request is sent`() {
        // Act
        DefaultMcpIntentDeepLinkHandler(
            queryParams = validParams + ("type" to "onramp"),
            mcpRequestService = mcpRequestService,
        )

        // Assert
        verify(exactly = 0) { mcpRequestService.request(any()) }
    }

    @Test
    fun `GIVEN express intent of an unknown operation WHEN handler created THEN no request is sent`() {
        // Act
        DefaultMcpIntentDeepLinkHandler(
            queryParams = validParams + ("type" to "refund"),
            mcpRequestService = mcpRequestService,
        )

        // Assert
        verify(exactly = 0) { mcpRequestService.request(any()) }
    }
}