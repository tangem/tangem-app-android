package com.tangem.features.feed.components.v2

import com.tangem.features.feed.nav.FeedTabId

/**
 * The single source of the feed tab order. Contributors declare only their id; the host lays the
 * tab row out in this sequence. A contributed tab missing from this list is skipped with an error
 * log — add its id here to show it.
 *
 * Becomes the static fallback once backend-driven ordering arrives.
 */
internal val FeedTabsOrder: List<FeedTabId> = listOf(
    FeedTabId.Crypto,
    FeedTabId.RealAssets,
    FeedTabId.Earn,
    FeedTabId.Predictions,
)