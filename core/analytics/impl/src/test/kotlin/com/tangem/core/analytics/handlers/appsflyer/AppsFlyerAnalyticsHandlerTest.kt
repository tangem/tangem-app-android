package com.tangem.core.analytics.handlers.appsflyer

import com.tangem.core.analytics.models.AnalyticsEvent
import com.tangem.core.analytics.models.AppsFlyerIncludedEvent
import com.tangem.core.analytics.models.AppsFlyerOnlyEvent
import io.mockk.clearMocks
import io.mockk.mockk
import io.mockk.verify
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Test

internal class AppsFlyerAnalyticsHandlerTest {

    private val client: AppsFlyerAnalyticsClient = mockk(relaxed = true)
    private val handler = AppsFlyerAnalyticsHandler(client)

    private val params = mapOf("Source" to "Main")

    @BeforeEach
    fun resetMocks() {
        clearMocks(client)
    }

    @Test
    fun `GIVEN AppsFlyer only event WHEN send THEN logs event id with params`() {
        // Act
        handler.send(OnlyEvent(params))

        // Assert
        verify(exactly = 1) { client.logEvent("[Test] Only", params) }
    }

    @Test
    fun `GIVEN included event without replacement WHEN send THEN logs event id with params`() {
        // Act
        handler.send(IncludedEvent(replacedEvent = null, params = params))

        // Assert
        verify(exactly = 1) { client.logEvent("[Test] Included", params) }
    }

    @Test
    fun `GIVEN included event with replacement WHEN send THEN logs replaced event in same category`() {
        // Act
        handler.send(IncludedEvent(replacedEvent = "Replaced", params = params))

        // Assert
        verify(exactly = 1) { client.logEvent("[Test] Replaced", params) }
    }

    @Test
    fun `GIVEN plain event WHEN send THEN nothing is logged`() {
        // Act
        handler.send(AnalyticsEvent(category = "Test", event = "Plain"))

        // Assert
        verify(exactly = 0) { client.logEvent(any(), any()) }
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

    private class OnlyEvent(params: Map<String, String>) :
        AnalyticsEvent(category = "Test", event = "Only", params = params), AppsFlyerOnlyEvent

    private class IncludedEvent(
        replacedEvent: String?,
        params: Map<String, String>,
    ) : AnalyticsEvent(category = "Test", event = "Included", params = params), AppsFlyerIncludedEvent {
        override val appsFlyerReplacedEvent: String? = replacedEvent
    }
}