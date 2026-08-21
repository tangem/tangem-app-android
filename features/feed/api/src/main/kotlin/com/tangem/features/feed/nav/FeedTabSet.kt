package com.tangem.features.feed.nav

/**
 * The feed's tabs, already ordered and filtered by availability — the single list every feed surface
 * renders.
 *
 * It exists because the feed home and the search screen each build their own pager over the same
 * tabs. Ordering and availability are host policy (`FeedTabsOrder` in `:features:feed:impl`), and a
 * second surface re-deriving that policy from the raw contributor set is how the two tab rows drift
 * out of sync.
 */
interface FeedTabSet {

    /** Contributed tabs in host order; a contributed tab absent from that order is skipped. */
    val tabs: List<FeedTabContributor>
}