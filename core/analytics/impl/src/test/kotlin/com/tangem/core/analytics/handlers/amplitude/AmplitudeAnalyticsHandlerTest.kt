package com.tangem.core.analytics.handlers.amplitude

import com.tangem.core.analytics.models.AnalyticsEvent
import io.mockk.clearMocks
import io.mockk.mockk
import io.mockk.verify
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Test

internal class AmplitudeAnalyticsHandlerTest {

    private val client: AmplitudeAnalyticsClient = mockk(relaxed = true)
    private val handler = AmplitudeAnalyticsHandler(client)

    @BeforeEach
    fun resetMocks() {
        clearMocks(client)
    }

    @Test
    fun `GIVEN event WHEN send THEN logs event id with params`() {
        // Arrange
        val event = AnalyticsEvent(category = "Basic", event = "Balance Loaded", params = mapOf("State" to "Full"))

        // Act
        handler.send(event)

        // Assert
        verify(exactly = 1) { client.logEvent("[Basic] Balance Loaded", mapOf("State" to "Full")) }
    }

    @Test
    fun `GIVEN user id WHEN setUserId THEN sets it on client`() {
        // Act
        handler.setUserId("user")

        // Assert
        verify(exactly = 1) { client.setUserId("user") }
    }

    @Test
    fun `GIVEN user id set WHEN clearUserId THEN clears it on client`() {
        // Act
        handler.clearUserId()

        // Assert
        verify(exactly = 1) { client.clearUserId() }
    }
}