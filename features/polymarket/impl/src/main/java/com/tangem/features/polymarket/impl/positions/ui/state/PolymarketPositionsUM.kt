package com.tangem.features.polymarket.impl.positions.ui.state

import androidx.compose.runtime.Immutable
import com.tangem.core.ui.components.containers.pullToRefresh.PullToRefreshConfig
import com.tangem.domain.polymarket.model.PolymarketPositionsFilter
import com.tangem.features.polymarket.impl.positiondetails.ui.state.PolymarketFinishedPositionUM
import kotlinx.collections.immutable.ImmutableList

@Immutable
internal data class PolymarketPositionsUM(
    val selectedTab: Tab,
    val content: Content,
    val onTabSelect: (Tab) -> Unit,
    val onBackClick: () -> Unit,
    val selectedFilter: PolymarketPositionsFilter = PolymarketPositionsFilter.ALL,
    val onFilterSelect: (PolymarketPositionsFilter) -> Unit = {},
    val finishedPosition: PolymarketFinishedPositionUM? = null,
    val onFinishedPositionDismiss: () -> Unit = {},
    val pullToRefreshConfig: PullToRefreshConfig = PullToRefreshConfig(isRefreshing = false, onRefresh = {}),
    val onRefreshRetry: (() -> Unit)? = null,
) {

    enum class Tab { Active, Finished }

    @Immutable
    data class FilteredContent(
        val filter: PolymarketPositionsFilter = PolymarketPositionsFilter.ALL,
        val content: Content = Content.Loading,
        val isRefreshing: Boolean = false,
        val onRefreshRetry: (() -> Unit)? = null,
    )

    @Immutable
    sealed interface Content {

        data object Loading : Content

        data object Empty : Content

        data class Error(val onRetry: () -> Unit) : Content

        data class Data(
            val positions: ImmutableList<PolymarketPositionUM>,
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
}