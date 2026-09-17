package com.tangem.features.feed.v2

import androidx.compose.runtime.Immutable

/**
 * [section] is optional and an unknown value degrades rather than fails: with no section the hub opens
 * on its default tab.
 */
@Immutable
data class FeedEntryRequest(val section: FeedSection? = null)