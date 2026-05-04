package com.tangem.features.promobanners.impl.analytics

import com.tangem.core.analytics.models.AnalyticsEvent

internal sealed class PromoBannerAnalyticsEvent(
    event: String,
    params: Map<String, String> = emptyMap(),
) : AnalyticsEvent(category = "Promo Banner", event = event, params = params) {

    data class Shown(
        private val displayId: Int,
        private val placeholder: String,
    ) : PromoBannerAnalyticsEvent(
        event = "Banner Shown",
        params = mapOf(
            PARAM_DISPLAY_ID to displayId.toString(),
            PARAM_PLACEHOLDER to placeholder,
        ),
    )

    data class CarouselScrolled(
        private val displayId: Int,
        private val placeholder: String,
    ) : PromoBannerAnalyticsEvent(
        event = "Banner Carousel Scrolled",
        params = mapOf(
            PARAM_DISPLAY_ID to displayId.toString(),
            PARAM_PLACEHOLDER to placeholder,
        ),
    )

    data class Clicked(
        private val displayId: Int,
        private val placeholder: String,
    ) : PromoBannerAnalyticsEvent(
        event = "Banner Button Clicked",
        params = mapOf(
            PARAM_DISPLAY_ID to displayId.toString(),
            PARAM_PLACEHOLDER to placeholder,
        ),
    )

    data class Dismissed(
        private val displayId: Int,
        private val placeholder: String,
    ) : PromoBannerAnalyticsEvent(
        event = "Banner Dismissed",
        params = mapOf(
            PARAM_DISPLAY_ID to displayId.toString(),
            PARAM_PLACEHOLDER to placeholder,
        ),
    )

    private companion object {
        const val PARAM_DISPLAY_ID = "Display Id"
        const val PARAM_PLACEHOLDER = "Placeholder"
    }
}