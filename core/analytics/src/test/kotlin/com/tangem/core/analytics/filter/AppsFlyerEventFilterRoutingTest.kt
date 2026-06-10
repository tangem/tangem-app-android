package com.tangem.core.analytics.filter

import com.google.common.truth.Truth.assertThat
import com.tangem.core.analytics.api.AnalyticsHandler
import com.tangem.core.analytics.models.AnalyticsEvent
import com.tangem.core.analytics.models.AppsFlyerIncludedEvent
import com.tangem.core.analytics.models.AppsFlyerOnlyEvent
import io.mockk.every
import io.mockk.mockk
import org.junit.jupiter.api.Test

internal class AppsFlyerEventFilterRoutingTest {

    private val filter = AppsFlyerEventFilter()

    private class AppsFlyerOnlyTestEvent :
        AnalyticsEvent(category = "Test", event = "OnlyEvent"), AppsFlyerOnlyEvent

    private class AppsFlyerIncludedTestEvent :
        AnalyticsEvent(category = "Test", event = "IncludedEvent"), AppsFlyerIncludedEvent

    @Test
    fun `withParams keeps AppsFlyerOnlyEvent routable to AppsFlyer`() {
        val event: AnalyticsEvent = AppsFlyerOnlyTestEvent()

        val enriched = event.withParams(mapOf("key" to "value"))

        assertThat(filter.canBeAppliedTo(enriched)).isTrue()
        assertThat(enriched.params).containsEntry("key", "value")
    }

    @Test
    fun `withParams keeps AppsFlyerIncludedEvent routable to AppsFlyer`() {
        val event: AnalyticsEvent = AppsFlyerIncludedTestEvent()

        val enriched = event.withParams(mapOf("key" to "value"))

        assertThat(filter.canBeAppliedTo(enriched)).isTrue()
        assertThat(enriched.params).containsEntry("key", "value")
    }

    @Test
    fun `enriched AppsFlyerOnlyEvent is consumable only by the AppsFlyer handler`() {
        val enriched = (AppsFlyerOnlyTestEvent() as AnalyticsEvent).withParams(emptyMap())

        val appsFlyerHandler = mockk<AnalyticsHandler> { every { id() } returns "AppsFlyer" }
        val otherHandler = mockk<AnalyticsHandler> { every { id() } returns "Amplitude" }

        assertThat(filter.canBeConsumedByHandler(appsFlyerHandler, enriched)).isTrue()
        assertThat(filter.canBeConsumedByHandler(otherHandler, enriched)).isFalse()
    }
}