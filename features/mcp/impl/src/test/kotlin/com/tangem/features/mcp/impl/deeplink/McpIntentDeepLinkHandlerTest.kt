package com.tangem.features.mcp.impl.deeplink

import android.net.Uri
import com.google.common.truth.Truth.assertThat
import com.tangem.common.routing.deeplink.DeepLinkRequest
import com.tangem.core.analytics.api.AnalyticsEventHandler
import com.tangem.core.analytics.models.AnalyticsEvent
import com.tangem.domain.mcp.McpRequestService
import com.tangem.domain.mcp.models.McpRequest
import com.tangem.domain.mcp.models.analytics.McpIntentAnalyticsEvent
import com.tangem.features.mcp.api.McpFeatureToggles
import io.mockk.clearMocks
import io.mockk.every
import io.mockk.mockk
import io.mockk.slot
import io.mockk.verify
import kotlinx.coroutines.CoroutineScope
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Test
import org.junit.jupiter.params.ParameterizedTest
import org.junit.jupiter.params.provider.CsvSource
import org.junit.jupiter.params.provider.ValueSource

internal class McpIntentDeepLinkHandlerTest {

    private val mcpRequestService: McpRequestService = mockk(relaxed = true)
    private val analyticsEventHandler: AnalyticsEventHandler = mockk(relaxed = true)
    private val mcpFeatureToggles: McpFeatureToggles = mockk()

    private val handler = McpIntentDeepLinkHandler(
        mcpRequestService = mcpRequestService,
        analyticsEventHandler = analyticsEventHandler,
        mcpFeatureToggles = mcpFeatureToggles,
    )

    private val exchangeSource = McpRequest.Intent.Source.Express(McpRequest.Intent.Source.Express.Type.EXCHANGE)
    private val sendSource = McpRequest.Intent.Source.Transaction(McpRequest.Intent.Source.Transaction.Type.SEND)

    private val validParams = mapOf("id" to "my_intent_id123", "source" to "express", "type" to "exchange")

    @BeforeEach
    fun resetMocks() {
        clearMocks(mcpRequestService, analyticsEventHandler, mcpFeatureToggles)
    }

    @ParameterizedTest
    @ValueSource(booleans = [true, false])
    fun `GIVEN mcp toggle WHEN isEnabled read THEN it follows the toggle`(isMcpEnabled: Boolean) {
        // Arrange
        every { mcpFeatureToggles.isMcpEnabled } returns isMcpEnabled

        // Act
        val actual = handler.isEnabled

        // Assert
        assertThat(actual).isEqualTo(isMcpEnabled)
    }

    @Test
    fun `GIVEN express exchange link WHEN handled THEN intent request is sent`() {
        // Act
        handle(queryParams = validParams)

        // Assert
        verify(exactly = 1) {
            mcpRequestService.request(McpRequest.Intent(id = "my_intent_id123", source = exchangeSource))
        }
        verify(exactly = 1) { analyticsEventHandler.send(intentReceived(exchangeSource)) }
    }

    @ParameterizedTest
    @ValueSource(strings = ["", " "])
    fun `GIVEN express link without type WHEN handled THEN it is taken as an exchange`(type: String) {
        // Arrange
        val params = validParams - "type" + ("type" to type)

        // Act
        handle(queryParams = params)

        // Assert
        verify(exactly = 1) {
            mcpRequestService.request(McpRequest.Intent(id = "my_intent_id123", source = exchangeSource))
        }
        verify(exactly = 1) { analyticsEventHandler.send(intentReceived(exchangeSource)) }
    }

    @Test
    fun `GIVEN express link with no type param WHEN handled THEN it is taken as an exchange`() {
        // Act
        handle(queryParams = validParams - "type")

        // Assert
        verify(exactly = 1) {
            mcpRequestService.request(McpRequest.Intent(id = "my_intent_id123", source = exchangeSource))
        }
        verify(exactly = 1) { analyticsEventHandler.send(intentReceived(exchangeSource)) }
    }

    @ParameterizedTest
    @ValueSource(strings = ["id", "source"])
    fun `GIVEN missing param WHEN handled THEN nothing is sent`(missingKey: String) {
        // Act
        handle(queryParams = validParams - missingKey)

        // Assert
        verify(exactly = 0) { mcpRequestService.request(any()) }
        verify(exactly = 0) { analyticsEventHandler.send(any()) }
    }

    @ParameterizedTest
    @ValueSource(strings = ["id", "source"])
    fun `GIVEN blank param WHEN handled THEN nothing is sent`(blankKey: String) {
        // Act
        handle(queryParams = validParams + (blankKey to " "))

        // Assert
        verify(exactly = 0) { mcpRequestService.request(any()) }
        verify(exactly = 0) { analyticsEventHandler.send(any()) }
    }

    @Test
    fun `GIVEN transaction send link WHEN handled THEN send intent request is sent`() {
        // Arrange
        val params = mapOf("id" to "my_intent_id123", "source" to "transaction", "type" to "send")

        // Act
        handle(queryParams = params)

        // Assert
        verify(exactly = 1) {
            mcpRequestService.request(McpRequest.Intent(id = "my_intent_id123", source = sendSource))
        }
        verify(exactly = 1) { analyticsEventHandler.send(intentReceived(sendSource)) }
    }

    @ParameterizedTest
    @ValueSource(strings = ["receive", "SEND", "", " "])
    fun `GIVEN transaction link of an unsupported or blank type WHEN handled THEN nothing is sent`(
        type: String,
    ) {
        // Act
        handle(queryParams = mapOf("id" to "my_intent_id123", "source" to "transaction", "type" to type))

        // Assert
        verify(exactly = 0) { mcpRequestService.request(any()) }
        verify(exactly = 0) { analyticsEventHandler.send(any()) }
    }

    @Test
    fun `GIVEN transaction link with no type param WHEN handled THEN nothing is sent`() {
        // Act
        handle(queryParams = mapOf("id" to "my_intent_id123", "source" to "transaction"))

        // Assert
        verify(exactly = 0) { mcpRequestService.request(any()) }
        verify(exactly = 0) { analyticsEventHandler.send(any()) }
    }

    @ParameterizedTest
    @ValueSource(strings = ["onramp", "staking"])
    fun `GIVEN intent of an unknown service WHEN handled THEN nothing is sent`(source: String) {
        // Act
        handle(queryParams = mapOf("id" to "my_intent_id123", "source" to source, "type" to "send"))

        // Assert
        verify(exactly = 0) { mcpRequestService.request(any()) }
        verify(exactly = 0) { analyticsEventHandler.send(any()) }
    }

    @Test
    fun `GIVEN express intent of an unsupported operation WHEN handled THEN nothing is sent`() {
        // Act
        handle(queryParams = validParams + ("type" to "onramp"))

        // Assert
        verify(exactly = 0) { mcpRequestService.request(any()) }
        verify(exactly = 0) { analyticsEventHandler.send(any()) }
    }

    @Test
    fun `GIVEN express intent of an unknown operation WHEN handled THEN nothing is sent`() {
        // Act
        handle(queryParams = validParams + ("type" to "refund"))

        // Assert
        verify(exactly = 0) { mcpRequestService.request(any()) }
        verify(exactly = 0) { analyticsEventHandler.send(any()) }
    }

    @ParameterizedTest
    @CsvSource("express, exchange", "transaction, send")
    fun `GIVEN supported intent link WHEN handled THEN intent received is sent once`(
        source: String,
        type: String,
    ) {
        // Arrange
        val event = slot<AnalyticsEvent>()
        val params = mapOf("id" to "my_intent_id123", "source" to source, "type" to type)

        // Act
        handle(queryParams = params)

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

    private fun handle(queryParams: Map<String, String>) = handler.handle(
        DeepLinkRequest(
            uri = mockk<Uri>(),
            queryParams = queryParams,
            scope = mockk<CoroutineScope>(),
            isFromOnNewIntent = false,
        ),
    )
}