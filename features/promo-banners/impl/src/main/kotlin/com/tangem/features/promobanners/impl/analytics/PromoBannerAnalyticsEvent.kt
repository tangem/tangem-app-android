package com.tangem.features.promobanners.impl.analytics

import com.tangem.core.analytics.models.AnalyticsEvent
import com.tangem.core.analytics.models.AnalyticsParam.Key.CAMPAIGN

internal sealed class PromoBannerAnalyticsEvent(
    event: String,
    params: Map<String, String> = emptyMap(),
) : AnalyticsEvent(category = "Promo Banner", event = event, params = params) {

    data class Shown(
        private val displayId: Int,
        private val placeholder: String,
        private val campaign: String? = null,
    ) : PromoBannerAnalyticsEvent(
        event = "Banner Shown",
        params = bannerParams(displayId, placeholder, campaign),
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
        private val campaign: String? = null,
    ) : PromoBannerAnalyticsEvent(
        event = "Banner Button Clicked",
        params = bannerParams(displayId, placeholder, campaign),
    )

    data class Dismissed(
        private val displayId: Int,
        private val placeholder: String,
        private val campaign: String? = null,
    ) : PromoBannerAnalyticsEvent(
        event = "Banner Dismissed",
        params = bannerParams(displayId, placeholder, campaign),
    )

    private companion object {
        const val PARAM_DISPLAY_ID = "Display Id"
        const val PARAM_PLACEHOLDER = "Placeholder"

        /** [campaign] is the name of the campaign behind the banner; banners without one don't send it. */
        fun bannerParams(displayId: Int, placeholder: String, campaign: String?): Map<String, String> = buildMap {
            put(PARAM_DISPLAY_ID, displayId.toString())
            put(PARAM_PLACEHOLDER, placeholder)
            campaign?.let { put(CAMPAIGN, it) }
        }
    }
}