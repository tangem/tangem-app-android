package com.tangem.features.feed.rwa.model.analytics

import com.tangem.core.analytics.models.AnalyticsEvent

/**
 * Events of the Real assets tab itself. Category `Markets`, next to the other shtorka events; the search
 * screen's own events live in [RwaSearchAnalyticsEvent] under `Search`.
 */
internal sealed class RwaAnalyticsEvent(
    event: String,
    params: Map<String, String> = emptyMap(),
) : AnalyticsEvent(category = "Markets", event = event, params = params) {

    /**
     * The notice about a category the user's region blocks was shown.
     *
     * @param categoryName backend `displayName` of the picked category — the spec enumerates the values by
     * name (`xStocks`, `Commodities`, …), not by id
     */
    data class RegionNotSupportedNotification(
        private val categoryName: String,
    ) : RwaAnalyticsEvent(
        event = "Notification - Region Not Supported",
        params = mapOf(
            SECTION_KEY to SECTION_ASSETS,
            CATEGORY_KEY to categoryName,
        ),
    )

    private companion object {
        const val SECTION_KEY = "Section"
        const val CATEGORY_KEY = "Category"

        /** This model backs the Real assets tab; the spec's other value, `Crypto`, belongs to the Crypto tab. */
        const val SECTION_ASSETS = "Assets"
    }
}