package com.tangem.features.polymarket.impl.history.ui.state

import androidx.compose.runtime.Immutable
import com.tangem.core.ui.ds2.search.TangemSearch
import com.tangem.core.ui.ds2.tabnavigation.TangemTabItemUM
import com.tangem.core.ui.extensions.TextReference
import kotlinx.collections.immutable.ImmutableList

@Immutable
internal data class PolymarketHistoryUM(
    val content: Content,
    val onBackClick: () -> Unit,
) {

    @Immutable
    sealed interface Content {

        data object Loading : Content

        data class Empty(val onExploreEventsClick: () -> Unit) : Content

        data class Error(val onRetry: () -> Unit) : Content

        data class Data(
            val groups: ImmutableList<DateGroup>,
            val tabs: ImmutableList<TangemTabItemUM>,
            val search: TangemSearch.State,
            val onDownloadCsvClick: () -> Unit,
        ) : Content
    }

    @Immutable
    data class DateGroup(
        val id: String,
        val title: TextReference,
        val activities: ImmutableList<PolymarketActivityRowUM>,
    )
}