package com.tangem.features.feed.components.v2

import com.tangem.features.feed.nav.FeedTabContributor
import com.tangem.features.feed.nav.FeedTabId
import com.tangem.features.feed.v2.FeedEntryRequest

/**
 * A request naming no section, or one whose tab this build does not show, falls back to the hub's
 * default section — whichever tab comes first. `null` only when the hub shows no tabs at all.
 */
internal fun FeedEntryRequest.resolveTab(tabs: List<FeedTabContributor>): FeedTabId? {
    val requested = section?.tabId?.takeIf { id -> tabs.any { it.id == id } }

    return requested ?: tabs.firstOrNull()?.id
}