package com.tangem.features.promobanners.impl.analytics

import com.tangem.core.analytics.models.AnalyticsEvent

internal sealed class PromoBannerAnalyticsEvent(
    event: String,
    params: Map<String, String> = emptyMap(),
) : AnalyticsEvent(category = "Promo Banner", event = event, params = params) {

    data class Shown(
        private val displayId: String,
        private val placeholder: String,
    ) : PromoBannerAnalyticsEvent(
        event = "Banner Shown",
        params = mapOf("Display Id" to displayId, "Placeholder" to placeholder),
    )

    data class CarouselScrolled(
        private val displayId: String,
        private val placeholder: String,
    ) : PromoBannerAnalyticsEvent(
        event = "Banner Carousel Scrolled",
        params = mapOf("Display Id" to displayId, "Placeholder" to placeholder),
    )

    data class Clicked(
        private val displayId: String,
        private val placeholder: String,
    ) : PromoBannerAnalyticsEvent(
        event = "Banner Button Clicked",
        params = mapOf("Display Id" to displayId, "Placeholder" to placeholder),
    )

    data class Dismissed(
        private val displayId: String,
        private val placeholder: String,
    ) : PromoBannerAnalyticsEvent(
        event = "Banner Dismissed",
        params = mapOf("Display Id" to displayId, "Placeholder" to placeholder),
    )
}