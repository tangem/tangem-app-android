package com.tangem.features.polymarket.impl.positions.ui.state

import androidx.compose.runtime.Immutable
import kotlinx.collections.immutable.ImmutableList

@Immutable
internal data class PolymarketPositionsUM(
    val selectedTab: Tab,
    val content: Content,
    val onTabSelect: (Tab) -> Unit,
    val onBackClick: () -> Unit,
) {

    enum class Tab { Active, Finished }

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