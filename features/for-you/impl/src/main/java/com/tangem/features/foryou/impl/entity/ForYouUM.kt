package com.tangem.features.foryou.impl.entity

import androidx.compose.runtime.Immutable
import com.tangem.core.ui.ds.row.token.TangemTokenRowUM
import com.tangem.core.ui.ds.tabs.TangemSegmentUM
import com.tangem.core.ui.ds.tabs.TangemSegmentedPickerUM
import com.tangem.features.foryou.impl.components.state.MarketChartUM
import com.tangem.features.foryou.impl.model.ForYouNotification
import kotlinx.collections.immutable.ImmutableList

internal data class ForYouUM(
    val portfolioReviewUM: PortfolioReviewUM,
    val notifications: ImmutableList<ForYouNotification>,
)

@Immutable
internal sealed interface PortfolioReviewUM {
    val tokenList: ImmutableList<ForYouTokenListItemUM>
    val marketChartUM: MarketChartUM

    data class Loading(
        override val tokenList: ImmutableList<ForYouTokenListItemUM>,
        override val marketChartUM: MarketChartUM.NoData,
    ) : PortfolioReviewUM

    data class Content(
        override val tokenList: ImmutableList<ForYouTokenListItemUM>,
        override val marketChartUM: MarketChartUM,
        val periodPickerUM: TangemSegmentedPickerUM,
        val onPeriodClick: (TangemSegmentUM) -> Unit,
    ) : PortfolioReviewUM
}

@Immutable
internal data class ForYouTokenListItemUM(
    val tokenRowUM: TangemTokenRowUM,
    val tokenList: ImmutableList<TangemTokenRowUM>,
    val isExpanded: Boolean,
    val isExpandable: Boolean,
)