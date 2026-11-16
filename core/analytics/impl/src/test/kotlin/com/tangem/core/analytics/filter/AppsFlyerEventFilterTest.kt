package com.tangem.core.analytics.filter

import com.google.common.truth.Truth.assertThat
import com.tangem.core.analytics.api.AnalyticsHandler
import com.tangem.core.analytics.models.AnalyticsEvent
import com.tangem.core.analytics.models.AppsFlyerIncludedEvent
import com.tangem.core.analytics.models.AppsFlyerOnlyEvent
import com.tangem.test.core.ProvideTestModels
import io.mockk.every
import io.mockk.mockk
import kotlinx.coroutines.test.runTest
import org.junit.jupiter.api.Nested
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.TestInstance
import org.junit.jupiter.params.ParameterizedTest

internal class AppsFlyerEventFilterTest {

    private val filter = AppsFlyerEventFilter()

    private val appsFlyerHandler: AnalyticsHandler = mockk { every { id() } returns "AppsFlyer" }
    private val amplitudeHandler: AnalyticsHandler = mockk { every { id() } returns "Amplitude" }

    @Nested
    @TestInstance(TestInstance.Lifecycle.PER_CLASS)
    inner class CanBeAppliedTo {

        @ParameterizedTest
        @ProvideTestModels
        fun `GIVEN event WHEN canBeAppliedTo THEN applies only to AppsFlyer events`(model: AppliedModel) {
            // Act
            val actual = filter.canBeAppliedTo(model.event)

            // Assert
            assertThat(actual).isEqualTo(model.expected)
        }

        private fun provideTestModels() = listOf(
            AppliedModel(event = AppsFlyerOnlyTestEvent(), expected = true),
            AppliedModel(event = AppsFlyerIncludedTestEvent(), expected = true),
            AppliedModel(event = AnalyticsEvent(category = "Test", event = "Plain"), expected = false),
        )
    }

    @Nested
    inner class CanBeSent {

        @Test
        fun `GIVEN AppsFlyer event WHEN canBeSent THEN returns true`() = runTest {
            // Act
            val actual = filter.canBeSent(AppsFlyerOnlyTestEvent())

            // Assert
            assertThat(actual).isTrue()
        }
    }

    @Nested
    @TestInstance(TestInstance.Lifecycle.PER_CLASS)
    inner class CanBeConsumedByHandler {

        @ParameterizedTest
        @ProvideTestModels
        fun `GIVEN event and handler WHEN canBeConsumedByHandler THEN routes by event type`(model: ConsumedModel) {
            // Act
            val actual = filter.canBeConsumedByHandler(model.handler, model.event)

            // Assert
            assertThat(actual).isEqualTo(model.expected)
        }

        private fun provideTestModels() = listOf(
            ConsumedModel(event = AppsFlyerOnlyTestEvent(), handler = appsFlyerHandler, expected = true),
            ConsumedModel(event = AppsFlyerOnlyTestEvent(), handler = amplitudeHandler, expected = false),
            ConsumedModel(event = AppsFlyerIncludedTestEvent(), handler = appsFlyerHandler, expected = true),
            ConsumedModel(event = AppsFlyerIncludedTestEvent(), handler = amplitudeHandler, expected = true),
            ConsumedModel(
                event = AnalyticsEvent(category = "Test", event = "Plain"),
                handler = appsFlyerHandler,
                expected = false,
            ),
        )
    }

    data class AppliedModel(val event: AnalyticsEvent, val expected: Boolean)

    data class ConsumedModel(val event: AnalyticsEvent, val handler: AnalyticsHandler, val expected: Boolean)

    private class AppsFlyerOnlyTestEvent :
        AnalyticsEvent(category = "Test", event = "OnlyEvent"), AppsFlyerOnlyEvent

    private class AppsFlyerIncludedTestEvent :
        AnalyticsEvent(category = "Test", event = "IncludedEvent"), AppsFlyerIncludedEvent
}