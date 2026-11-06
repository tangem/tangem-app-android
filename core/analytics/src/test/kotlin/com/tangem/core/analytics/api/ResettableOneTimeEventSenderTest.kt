package com.tangem.core.analytics.api

import com.tangem.core.analytics.models.AnalyticsEvent
import io.mockk.clearMocks
import io.mockk.mockk
import io.mockk.verify
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Test

internal class ResettableOneTimeEventSenderTest {

    private val analyticsEventHandler: AnalyticsEventHandler = mockk(relaxed = true)
    private val event = AnalyticsEvent(category = "Test", event = "Event")

    @BeforeEach
    fun resetMocks() {
        clearMocks(analyticsEventHandler)
    }

    @Test
    fun `GIVEN key not sent WHEN sendEventOnce THEN event is sent`() {
        // Arrange
        val sender = ResettableOneTimeEventSender(analyticsEventHandler)

        // Act
        sender.sendEventOnce(key = "key", event = event)

        // Assert
        verify(exactly = 1) { analyticsEventHandler.send(event) }
    }

    @Test
    fun `GIVEN key already sent WHEN sendEventOnce THEN event is not sent again`() {
        // Arrange
        val sender = ResettableOneTimeEventSender(analyticsEventHandler)
        sender.sendEventOnce(key = "key", event = event)

        // Act
        sender.sendEventOnce(key = "key", event = AnalyticsEvent(category = "Test", event = "Other"))

        // Assert
        verify(exactly = 1) { analyticsEventHandler.send(any()) }
    }

    @Test
    fun `GIVEN key already sent WHEN sendEventOnce with another key THEN event is sent`() {
        // Arrange
        val sender = ResettableOneTimeEventSender(analyticsEventHandler)
        sender.sendEventOnce(key = "key", event = event)

        // Act
        sender.sendEventOnce(key = "another", event = event)

        // Assert
        verify(exactly = 2) { analyticsEventHandler.send(event) }
    }

    @Test
    fun `GIVEN key reset WHEN sendEventOnce THEN event is sent again`() {
        // Arrange
        val sender = ResettableOneTimeEventSender(analyticsEventHandler)
        sender.sendEventOnce(key = "key", event = event)

        // Act
        sender.reset(key = "key")
        sender.sendEventOnce(key = "key", event = event)

        // Assert
        verify(exactly = 2) { analyticsEventHandler.send(event) }
    }
}