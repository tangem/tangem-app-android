package com.tangem.features.feed.search.ui.state

import androidx.compose.runtime.Immutable

@Immutable
internal data class FeedSearchUM(
    val source: String,
    val query: String = "",
)