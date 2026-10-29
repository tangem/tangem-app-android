package com.tangem.core.analytics.filter

import com.google.common.truth.Truth.assertThat
import com.tangem.core.analytics.api.OneTimeEventStore
import com.tangem.core.analytics.models.AnalyticsEvent
import com.tangem.core.analytics.models.OneTimeAnalyticsEvent
import io.mockk.clearMocks
import io.mockk.coEvery
import io.mockk.coVerify
import io.mockk.mockk
import kotlinx.coroutines.test.runTest
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Test

internal class OneTimeEventFilterTest {

    private val oneTimeEventStore: OneTimeEventStore = mockk()

    private val filter = OneTimeEventFilter(oneTimeEventStore = oneTimeEventStore)

    @BeforeEach
    fun resetMocks() {
        clearMocks(oneTimeEventStore)
    }

    @Test
    fun `GIVEN one-time event not sent before WHEN canBeSent THEN true`() = runTest {
        // Arrange
        coEvery { oneTimeEventStore.markSent(ONE_TIME_EVENT_ID) } returns false

        // Act
        val actual = filter.canBeSent(OneTimeTestEvent())

        // Assert
        assertThat(actual).isTrue()
    }

    @Test
    fun `GIVEN one-time event sent before WHEN canBeSent THEN false`() = runTest {
        // Arrange
        coEvery { oneTimeEventStore.markSent(ONE_TIME_EVENT_ID) } returns true

        // Act
        val actual = filter.canBeSent(OneTimeTestEvent())

        // Assert
        assertThat(actual).isFalse()
    }

    @Test
    fun `GIVEN regular event WHEN canBeSent THEN true without touching the store`() = runTest {
        // Act
        val actual = filter.canBeSent(AnalyticsEvent(category = "Test", event = "Regular"))

        // Assert
        assertThat(actual).isTrue()
        coVerify(exactly = 0) { oneTimeEventStore.markSent(any()) }
    }

    private class OneTimeTestEvent :
        AnalyticsEvent(category = "Test", event = "OneTime"), OneTimeAnalyticsEvent {
        override val oneTimeEventId: String = ONE_TIME_EVENT_ID
    }

    private companion object {
        const val ONE_TIME_EVENT_ID = "one_time"
    }
}