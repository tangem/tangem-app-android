package com.tangem.features.promobanners.impl.analytics

import com.tangem.core.analytics.models.AnalyticsEvent

internal sealed class PromoBannerAnalyticsEvent(
    event: String,
    params: Map<String, String> = emptyMap(),
) : AnalyticsEvent(category = "Banner", event = event, params = params) {

    data class Shown(val bannerId: String) : PromoBannerAnalyticsEvent(
        event = "Banner Shown",
        params = mapOf("banner_id" to bannerId),
    )

    data class Clicked(val bannerId: String) : PromoBannerAnalyticsEvent(
        event = "Banner Clicked",
        params = mapOf("banner_id" to bannerId),
    )

    data class Dismissed(val bannerId: String) : PromoBannerAnalyticsEvent(
        event = "Banner Dismissed",
        params = mapOf("banner_id" to bannerId),
    )
}