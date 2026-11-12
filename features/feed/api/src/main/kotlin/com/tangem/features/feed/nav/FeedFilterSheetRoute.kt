package com.tangem.features.feed.nav

import com.tangem.features.feed.v2.FeedFilterSheet

/**
 * A tab's filter picker on the feed stack. A deeplink asking for this same picker again leaves it open and
 * only changes its value, rather than having the host clear the stack and the tab reopen it.
 */
interface FeedFilterSheetRoute : FeedRoute {
    val tabId: FeedTabId
    val sheet: FeedFilterSheet
}