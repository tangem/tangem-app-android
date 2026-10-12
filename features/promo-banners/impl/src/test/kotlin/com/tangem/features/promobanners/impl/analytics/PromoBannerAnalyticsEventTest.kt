package com.tangem.features.promobanners.impl.analytics

import com.google.common.truth.Truth.assertThat
import com.tangem.core.analytics.models.AnalyticsEvent
import com.tangem.test.core.ProvideTestModels
import org.junit.jupiter.api.TestInstance
import org.junit.jupiter.params.ParameterizedTest

@TestInstance(TestInstance.Lifecycle.PER_CLASS)
internal class PromoBannerAnalyticsEventTest {

    @ParameterizedTest
    @ProvideTestModels
    fun `GIVEN banner event WHEN created THEN campaign sent only for campaign banners`(model: EventModel) {
        // Act
        val event = model.create(model.campaign)

        // Assert
        assertThat(event.category).isEqualTo("Promo Banner")
        assertThat(event.event).isEqualTo(model.expectedEvent)
        assertThat(event.params).isEqualTo(model.expectedParams)
    }

    private fun provideTestModels(): List<EventModel> {
        val events = listOf<Triple<String, String, (String?) -> AnalyticsEvent>>(
            Triple("Shown", "Banner Shown") { name -> PromoBannerAnalyticsEvent.Shown(7, "main", name) },
            Triple("Clicked", "Banner Button Clicked") { name -> PromoBannerAnalyticsEvent.Clicked(7, "main", name) },
            Triple("Dismissed", "Banner Dismissed") { name -> PromoBannerAnalyticsEvent.Dismissed(7, "main", name) },
        )
        val baseParams = mapOf("Display Id" to "7", "Placeholder" to "main")

        return events.flatMap { (name, eventName, create) ->
            listOf(
                EventModel(
                    name = "$name of a campaign banner",
                    create = create,
                    campaign = "whale-swap-cashback",
                    expectedEvent = eventName,
                    expectedParams = baseParams + ("Campaign" to "whale-swap-cashback"),
                ),
                EventModel(
                    name = "$name of a plain banner",
                    create = create,
                    campaign = null,
                    expectedEvent = eventName,
                    expectedParams = baseParams,
                ),
            )
        }
    }

    internal data class EventModel(
        val name: String,
        val create: (String?) -> AnalyticsEvent,
        val campaign: String?,
        val expectedEvent: String,
        val expectedParams: Map<String, String>,
    ) {
        override fun toString(): String = name
    }
}