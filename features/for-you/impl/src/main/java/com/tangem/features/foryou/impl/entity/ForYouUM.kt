package com.tangem.features.foryou.impl.entity

import androidx.compose.runtime.Immutable
import com.tangem.core.ui.ds.row.token.TangemTokenRowUM
import com.tangem.core.ui.ds.tabs.TangemSegmentUM
import com.tangem.core.ui.ds.tabs.TangemSegmentedPickerUM
import com.tangem.core.ui.extensions.TextReference
import com.tangem.features.commonfeatures.api.choosetoken.model.WalletListUM
import kotlinx.collections.immutable.ImmutableList

internal data class ForYouUM(
    val walletListUM: WalletListUM,
    val portfolioReviewUM: PortfolioReviewUM,
)

@Immutable
internal sealed interface PortfolioReviewUM {
    val tokenList: ImmutableList<ForYouTokenListItemUM>

    data class Loading(
        override val tokenList: ImmutableList<ForYouTokenListItemUM>,
    ) : PortfolioReviewUM

    data class Content(
        override val tokenList: ImmutableList<ForYouTokenListItemUM>,
        val periodPickerUM: TangemSegmentedPickerUM,
        val assetCount: TextReference,
        val topHoldingPercent: TextReference,
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