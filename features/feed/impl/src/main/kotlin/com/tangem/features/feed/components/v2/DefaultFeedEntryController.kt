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

    override fun request(request: FeedEntryRequest) {
        pendingEntry.value = request
        pendingFilter.value = request.toFilter()
    }

    override fun consumeEntry() {
        pendingEntry.value = null
    }

    override fun consumeFilter() {
        pendingFilter.value = null
    }

    /**
     * A filter is only ever addressed to the requested section's tab: with no section there is no
     * addressee, and an empty filter would park a slot nobody would ever come to clear.
     */
    private fun FeedEntryRequest.toFilter(): FeedEntryFilter? {
        val tabId = section?.tabId ?: return null

        return FeedEntryFilter(tabId = tabId, categoryId = categoryId, earnType = earnType)
            .takeIf { it.categoryId != null || it.earnType != null }
    }
}