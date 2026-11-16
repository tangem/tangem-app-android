package com.tangem.core.analytics.handlers.customerio

import com.tangem.core.analytics.models.AnalyticsEvent
import io.mockk.Called
import io.mockk.clearMocks
import io.mockk.mockk
import io.mockk.verify
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Test

internal class CustomerIoAnalyticsHandlerTest {

    private val client: CustomerIoAnalyticsClient = mockk(relaxed = true)
    private val handler = CustomerIoAnalyticsHandler(client)

    @BeforeEach
    fun resetMocks() {
        clearMocks(client)
    }

    @Test
    fun `GIVEN event WHEN send THEN nothing reaches client`() {
        // Act
        handler.send(AnalyticsEvent(category = "Basic", event = "Balance Loaded"))

        // Assert
        verify { client wasNot Called }
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