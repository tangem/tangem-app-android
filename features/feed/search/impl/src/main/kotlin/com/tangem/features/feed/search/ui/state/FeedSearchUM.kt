package com.tangem.features.feed.search.ui.state

import androidx.compose.runtime.Immutable
import kotlinx.collections.immutable.ImmutableList
import kotlinx.collections.immutable.persistentListOf

@Immutable
internal data class FeedSearchUM(
    val source: String,
    val query: String = "",
    val recentQueries: ImmutableList<RecentQueryUM> = persistentListOf(),
)

@Immutable
internal data class RecentQueryUM(
    val text: String,
    val onClick: () -> Unit,
    val onRemoveClick: () -> Unit,
)