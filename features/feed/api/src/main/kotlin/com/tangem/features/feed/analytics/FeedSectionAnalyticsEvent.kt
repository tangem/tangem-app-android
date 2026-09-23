package com.tangem.features.feed.analytics

import com.tangem.core.analytics.models.AnalyticsEvent
import com.tangem.core.analytics.models.AnalyticsParam.Key.CATEGORY
import com.tangem.core.analytics.models.AnalyticsParam.Key.PERIOD
import com.tangem.core.analytics.models.AnalyticsParam.Key.SECTION
import com.tangem.core.analytics.models.AnalyticsParam.Key.TYPE

/**
 * Events shared by every feed section, told apart by [SECTION].
 *
 * They live in the api module because the feed host reports the section-level ones while each tab
 * module reports its own interactions, and both halves have to agree on the wire format.
 */
sealed class FeedSectionAnalyticsEvent(
    event: String,
    params: Map<String, String> = emptyMap(),
) : AnalyticsEvent(category = "Markets", event = event, params = params) {

    data class MarketsScreenOpened(val section: String) : FeedSectionAnalyticsEvent(
        event = "Markets Screen Opened",
        params = mapOf(SECTION to section),
    )

    data class EnterSection(val section: String) : FeedSectionAnalyticsEvent(
        event = "Enter Section",
        params = mapOf(SECTION to section),
    )

    data class SortBy(
        val section: String,
        val type: FeedSortType,
        val period: FeedSortPeriod,
    ) : FeedSectionAnalyticsEvent(
        event = "Sort By",
        params = mapOf(
            SECTION to section,
            TYPE to type.analyticsValue,
            PERIOD to period.analyticsValue,
        ),
    )

    data class SelectCategory(val section: String, val categoryId: String) : FeedSectionAnalyticsEvent(
        event = "Select Category",
        params = mapOf(
            SECTION to section,
            CATEGORY to categoryId,
        ),
    )

    companion object {

        /** Reported when the user clears the category filter — both selectors model that as no category. */
        const val ALL_CATEGORIES = "All"
    }
}

/**
 * Sort options as Amplitude names them. Every section maps its own sort enum onto this one so the
 * `Sort By` funnel stays a single series; the values reproduce the pre-tabs markets list, where
 * "sort by market cap" was already reported as [Rating].
 */
enum class FeedSortType(val analyticsValue: String) {
    Rating("Rating"),
    Trending("Trending"),
    Buyers("Buyers"),
    Gainers("Gainers"),
    Losers("Losers"),
}

/** Price-trend interval as Amplitude names it. */
enum class FeedSortPeriod(val analyticsValue: String) {
    H24("24h"),
    D7("7d"),
    M1("1m"),
}