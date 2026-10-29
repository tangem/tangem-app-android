package com.tangem.features.mcp.impl.deeplink

import com.google.common.truth.Truth.assertThat
import com.tangem.core.analytics.api.AnalyticsEventHandler
import com.tangem.core.analytics.models.AnalyticsEvent
import com.tangem.domain.mcp.McpRequestService
import com.tangem.domain.mcp.models.McpRequest
import com.tangem.domain.mcp.models.analytics.McpIntentAnalyticsEvent
import io.mockk.clearMocks
import io.mockk.mockk
import io.mockk.slot
import io.mockk.verify
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Test
import org.junit.jupiter.params.ParameterizedTest
import org.junit.jupiter.params.provider.CsvSource
import org.junit.jupiter.params.provider.ValueSource

internal class DefaultMcpIntentDeepLinkHandlerTest {

    private val mcpRequestService: McpRequestService = mockk(relaxed = true)
    private val analyticsEventHandler: AnalyticsEventHandler = mockk(relaxed = true)

    private val exchangeSource = McpRequest.Intent.Source.Express(McpRequest.Intent.Source.Express.Type.EXCHANGE)
    private val sendSource = McpRequest.Intent.Source.Transaction(McpRequest.Intent.Source.Transaction.Type.SEND)

    private val validParams = mapOf("id" to "my_intent_id123", "source" to "express", "type" to "exchange")

    @BeforeEach
    fun resetMocks() {
        clearMocks(mcpRequestService, analyticsEventHandler)
    }

    @Test
    fun `GIVEN express exchange link WHEN handler created THEN intent request is sent`() {
        // Act
        createHandler(queryParams = validParams)

        // Assert
        verify(exactly = 1) {
            mcpRequestService.request(McpRequest.Intent(id = "my_intent_id123", source = exchangeSource))
        }
        verify(exactly = 1) { analyticsEventHandler.send(intentReceived(exchangeSource)) }
    }

    @ParameterizedTest
    @ValueSource(strings = ["", " "])
    fun `GIVEN express link without type WHEN handler created THEN it is taken as an exchange`(type: String) {
        // Arrange
        val params = validParams - "type" + ("type" to type)

        // Act
        createHandler(queryParams = params)

        // Assert
        verify(exactly = 1) {
            mcpRequestService.request(McpRequest.Intent(id = "my_intent_id123", source = exchangeSource))
        }
        verify(exactly = 1) { analyticsEventHandler.send(intentReceived(exchangeSource)) }
    }

    @Test
    fun `GIVEN express link with no type param WHEN handler created THEN it is taken as an exchange`() {
        // Act
        createHandler(queryParams = validParams - "type")

        // Assert
        verify(exactly = 1) {
            mcpRequestService.request(McpRequest.Intent(id = "my_intent_id123", source = exchangeSource))
        }
        verify(exactly = 1) { analyticsEventHandler.send(intentReceived(exchangeSource)) }
    }

    @ParameterizedTest
    @ValueSource(strings = ["id", "source"])
    fun `GIVEN missing param WHEN handler created THEN nothing is sent`(missingKey: String) {
        // Act
        createHandler(queryParams = validParams - missingKey)

        // Assert
        verify(exactly = 0) { mcpRequestService.request(any()) }
        verify(exactly = 0) { analyticsEventHandler.send(any()) }
    }

    @ParameterizedTest
    @ValueSource(strings = ["id", "source"])
    fun `GIVEN blank param WHEN handler created THEN nothing is sent`(blankKey: String) {
        // Act
        createHandler(queryParams = validParams + (blankKey to " "))

        // Assert
        verify(exactly = 0) { mcpRequestService.request(any()) }
        verify(exactly = 0) { analyticsEventHandler.send(any()) }
    }

    @Test
    fun `GIVEN transaction send link WHEN handler created THEN send intent request is sent`() {
        // Arrange
        val params = mapOf("id" to "my_intent_id123", "source" to "transaction", "type" to "send")

        // Act
        createHandler(queryParams = params)

        // Assert
        verify(exactly = 1) {
            mcpRequestService.request(McpRequest.Intent(id = "my_intent_id123", source = sendSource))
        }
        verify(exactly = 1) { analyticsEventHandler.send(intentReceived(sendSource)) }
    }

    @ParameterizedTest
    @ValueSource(strings = ["receive", "SEND", "", " "])
    fun `GIVEN transaction link of an unsupported or blank type WHEN handler created THEN nothing is sent`(
        type: String,
    ) {
        // Act
        createHandler(queryParams = mapOf("id" to "my_intent_id123", "source" to "transaction", "type" to type))

        // Assert
        verify(exactly = 0) { mcpRequestService.request(any()) }
        verify(exactly = 0) { analyticsEventHandler.send(any()) }
    }

    @Test
    fun `GIVEN transaction link with no type param WHEN handler created THEN nothing is sent`() {
        // Act
        createHandler(queryParams = mapOf("id" to "my_intent_id123", "source" to "transaction"))

        // Assert
        verify(exactly = 0) { mcpRequestService.request(any()) }
        verify(exactly = 0) { analyticsEventHandler.send(any()) }
    }

    @ParameterizedTest
    @ValueSource(strings = ["onramp", "staking"])
    fun `GIVEN intent of an unknown service WHEN handler created THEN nothing is sent`(source: String) {
        // Act
        createHandler(queryParams = mapOf("id" to "my_intent_id123", "source" to source, "type" to "send"))

        // Assert
        verify(exactly = 0) { mcpRequestService.request(any()) }
        verify(exactly = 0) { analyticsEventHandler.send(any()) }
    }

    @Test
    fun `GIVEN express intent of an unsupported operation WHEN handler created THEN nothing is sent`() {
        // Act
        createHandler(queryParams = validParams + ("type" to "onramp"))

        // Assert
        verify(exactly = 0) { mcpRequestService.request(any()) }
        verify(exactly = 0) { analyticsEventHandler.send(any()) }
    }

    @Test
    fun `GIVEN express intent of an unknown operation WHEN handler created THEN nothing is sent`() {
        // Act
        createHandler(queryParams = validParams + ("type" to "refund"))

        // Assert
        verify(exactly = 0) { mcpRequestService.request(any()) }
        verify(exactly = 0) { analyticsEventHandler.send(any()) }
    }

    @ParameterizedTest
    @CsvSource("express, exchange", "transaction, send")
    fun `GIVEN supported intent link WHEN handler created THEN intent received is sent once`(
        source: String,
        type: String,
    ) {
        // Arrange
        val event = slot<AnalyticsEvent>()
        val params = mapOf("id" to "my_intent_id123", "source" to source, "type" to type)

        // Act
        createHandler(queryParams = params)

        // Assert
        verify(exactly = 1) { analyticsEventHandler.send(capture(event)) }
        assertThat(event.captured).isInstanceOf(McpIntentAnalyticsEvent.IntentReceived::class.java)
        assertThat(event.captured.id).isEqualTo("[MCP Intent] Intent Received")
        assertThat(event.captured.params).isEqualTo(
            mapOf("Intent Type" to type, "Source" to source, "Intent Id" to "my_intent_id123"),
        )
    }



    private fun intentReceived(source: McpRequest.Intent.Source) =
        McpIntentAnalyticsEvent.IntentReceived(intentId = "my_intent_id123", source = source)

    private fun createHandler(queryParams: Map<String, String>) = DefaultMcpIntentDeepLinkHandler(
        queryParams = queryParams,
        mcpRequestService = mcpRequestService,
        analyticsEventHandler = analyticsEventHandler,
    )
}