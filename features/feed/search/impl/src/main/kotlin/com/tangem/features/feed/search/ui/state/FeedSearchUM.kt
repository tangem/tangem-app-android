package com.tangem.features.feed.search.ui.state

import androidx.compose.runtime.Immutable
import kotlinx.collections.immutable.ImmutableList

@Immutable
internal data class FeedSearchUM(
    val source: String,
    val query: String = "",
    val recent: RecentUM? = null,
)

@Immutable
internal data class RecentUM(
    val queries: ImmutableList<QueryUM>,
    val onClearClick: () -> Unit,
) {

    @Immutable
    data class QueryUM(
        val text: String,
        val onClick: () -> Unit,
        val onRemoveClick: () -> Unit,
    )
}