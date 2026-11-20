package com.tangem.features.feed.components.v2

import com.tangem.features.feed.v2.FeedEntryController
import com.tangem.features.feed.v2.FeedEntryFilter
import com.tangem.features.feed.v2.FeedEntryRequest
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
internal class DefaultFeedEntryController @Inject constructor() : FeedEntryController {

    override val pendingEntry: StateFlow<FeedEntryRequest?>
        field = MutableStateFlow<FeedEntryRequest?>(null)

    override val pendingFilter: StateFlow<FeedEntryFilter?>
        field = MutableStateFlow<FeedEntryFilter?>(null)

    /** Outlives consumption: a tab still loading for its filter asks about it after both slots are cleared. */
    @Volatile
    private var latestFilter: FeedEntryFilter? = null

    /**
     * [latestFilter] is set before either slot is published: a tab collecting on another dispatcher
     * consumes the filter as soon as it lands, so reading the slot back afterwards could see `null`.
     */
    override fun request(request: FeedEntryRequest) {
        val filter = request.toFilter()
        // an equal filter still pending is kept by the StateFlow, and that one is what the tab receives
        latestFilter = pendingFilter.value?.takeIf { it == filter } ?: filter
        pendingEntry.value = request
        pendingFilter.value = latestFilter
    }

    override fun consumeEntry() {
        pendingEntry.value = null
    }

    override fun consumeFilter() {
        pendingFilter.value = null
    }

    override fun isLatest(filter: FeedEntryFilter): Boolean = filter === latestFilter

    /**
     * A filter is only ever addressed to the requested section's tab: with no section there is no
     * addressee, and an empty filter would park a slot nobody would ever come to clear.
     */
    private fun FeedEntryRequest.toFilter(): FeedEntryFilter? {
        val tabId = section?.tabId ?: return null

        return FeedEntryFilter(tabId = tabId, categoryId = categoryId, earnType = earnType, sheet = sheet)
            .takeIf { it.categoryId != null || it.earnType != null || it.sheet != null }
    }
}