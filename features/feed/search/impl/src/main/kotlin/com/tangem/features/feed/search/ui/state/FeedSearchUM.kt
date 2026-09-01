package com.tangem.features.feed.search.ui.state

import androidx.compose.runtime.Immutable
import com.tangem.core.ui.ds2.tokenicon.TangemTokenIcon
import kotlinx.collections.immutable.ImmutableList

@Immutable
internal data class FeedSearchUM(
    val source: String,
    val query: String = "",
    val recent: RecentUM? = null,
)

@Immutable
internal data class RecentUM(
    val items: ImmutableList<ItemUM>,
    val queries: ImmutableList<QueryUM>,
    val onClearClick: () -> Unit,
) {

    @Immutable
    data class ItemUM(
        val id: String,
        val title: String,
        val icon: TangemTokenIcon.UiState,
        val onClick: () -> Unit,
    )

    @Immutable
    data class QueryUM(
        val text: String,
        val onClick: () -> Unit,
        val onRemoveClick: () -> Unit,
    )
}