package com.tangem.tap.common.analytics.handlers.firebase

import com.tangem.core.analytics.models.ExceptionAnalyticsEvent
import com.tangem.tap.common.analytics.events.AnalyticsParam
import io.mockk.clearMocks
import io.mockk.mockk
import io.mockk.verify
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Test

internal class FirebaseAnalyticsHandlerTest {

    private val client: FirebaseAnalyticsClient = mockk(relaxed = true)
    private val handler = FirebaseAnalyticsHandler(client)

    @BeforeEach
    fun resetMocks() {
        clearMocks(client)
    }

    @Test
    fun `GIVEN exception with params WHEN sendException THEN error details are sent with event params`() {
        // Arrange
        val exception = IllegalStateException("boom")
        val event = ExceptionAnalyticsEvent(exception = exception, params = mapOf("Screen" to "Main"))

        // Act
        handler.sendException(event)

        // Assert
        val expectedParams = mapOf(
            AnalyticsParam.ERROR_KEY to AnalyticsParam.Error.App.value,
            AnalyticsParam.ERROR_DESCRIPTION to "boom",
            "Screen" to "Main",
        )
        verify(exactly = 1) { client.logException(exception, expectedParams) }
    }
}