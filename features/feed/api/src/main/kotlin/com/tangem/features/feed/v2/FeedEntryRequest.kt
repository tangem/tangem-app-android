package com.tangem.features.feed.v2

import androidx.compose.runtime.Immutable
import com.tangem.domain.models.earn.PreselectedEarnType
import com.tangem.features.feed.nav.FeedTabId

/**
 * Every field is optional and every unknown value degrades rather than fails: no [section] opens the
 * hub's default tab, and a filter the addressed tab cannot resolve leaves that tab unfiltered.
 */
@Immutable
data class FeedEntryRequest(
    val section: FeedSection? = null,
    val categoryId: String? = null,
    val earnType: PreselectedEarnType? = null,
)

/**
 * Only [tabId]'s tab reads this half of a [FeedEntryRequest], and that same tab clears it — including
 * when it rejects the value as unknown, so an unresolvable filter is not retried.
 */
@Immutable
data class FeedEntryFilter(
    val tabId: FeedTabId,
    val categoryId: String? = null,
    val earnType: PreselectedEarnType? = null,
)