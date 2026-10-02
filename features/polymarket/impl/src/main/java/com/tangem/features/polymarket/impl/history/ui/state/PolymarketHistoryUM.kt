package com.tangem.features.polymarket.impl.history.ui.state

import androidx.compose.runtime.Immutable
import com.tangem.core.ui.components.containers.pullToRefresh.PullToRefreshConfig
import com.tangem.core.ui.ds2.search.TangemSearch
import com.tangem.core.ui.ds2.tabnavigation.TangemTabItemUM
import com.tangem.core.ui.extensions.TextReference
import kotlinx.collections.immutable.ImmutableList
import kotlinx.collections.immutable.persistentListOf

@Immutable
internal data class PolymarketHistoryUM(
    val content: Content,
    val onBackClick: () -> Unit,
    val tabs: ImmutableList<TangemTabItemUM> = persistentListOf(),
    val filterKey: String = "ALL",
    val pullToRefreshConfig: PullToRefreshConfig = PullToRefreshConfig(isRefreshing = false, onRefresh = {}),
) {

    @Immutable
    sealed interface Content {

        data object Loading : Content

        data object NoResults : Content

        data class Empty(val onExploreEventsClick: () -> Unit) : Content

        data class Error(val onRetry: () -> Unit) : Content

        data class Data(
            val groups: ImmutableList<DateGroup>,
            val search: TangemSearch.State,
            val onDownloadCsvClick: () -> Unit,
            val nextPage: NextPage = NextPage.End,
        ) : Content
    }

    @Immutable
    sealed interface NextPage {
        data object End : NextPage

        data object Loading : NextPage

        data class Ready(val onLoadMore: () -> Unit) : NextPage

        data class Error(val onRetry: () -> Unit) : NextPage
    }

    @Immutable
    data class DateGroup(
        val id: String,
        val title: TextReference,
        val activities: ImmutableList<PolymarketActivityRowUM>,
    )
}