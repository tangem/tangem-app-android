package com.tangem.features.foryou.impl.entity

import androidx.annotation.StringRes
import androidx.compose.runtime.Immutable
import com.tangem.core.ui.ds.image.DeviceIconUM
import com.tangem.core.ui.ds.row.token.TangemTokenRowUM
import com.tangem.core.ui.ds.tabs.TangemSegmentUM
import com.tangem.core.ui.ds.tabs.TangemSegmentedPickerUM
import com.tangem.core.ui.ds2.filter.TangemFilterItemUM
import com.tangem.core.ui.extensions.TextReference
import com.tangem.features.foryou.impl.components.state.DonutSegmentColor
import com.tangem.features.foryou.impl.components.state.MarketChartUM
import com.tangem.features.foryou.impl.model.ForYouNotification
import kotlinx.collections.immutable.ImmutableList
import kotlinx.collections.immutable.persistentListOf

internal data class ForYouUM(
    val portfolioReviewUM: PortfolioReviewUM,
    val earnOpportunities: EarnOpportunitiesUM,
    val notifications: ImmutableList<ForYouNotification>,
    val periodPickerUM: TangemSegmentedPickerUM,
    val onPeriodClick: (tangemSegmentUM: TangemSegmentUM) -> Unit,
    val portfolioFilter: TangemFilterItemUM,
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
        val onAddFundsClick: (() -> Unit)?,
    ) : PortfolioReviewUM
}

/**
 * State of the earn-opportunities section. [tokenList] holds either the user's earn-eligible holdings
 * or top-earn suggestions, depending on which content state was picked (see
 * `ForYouEarnOpportunitiesConverter` for the selection rules).
 */
@Immutable
internal sealed interface EarnOpportunitiesUM {

    val tokenList: ImmutableList<ForYouWalletGroupUM>

    /** Skeleton rows shown until the first real emission. */
    data class Loading(
        override val tokenList: ImmutableList<ForYouWalletGroupUM>,
    ) : EarnOpportunitiesUM

    /**
     * @property subtitleRes section subtitle matching the picked state (nothing eligible / all active /
     * potential rewards)
     * @property potentialReward header value: the total projected yearly reward for eligible holdings,
     * or the best suggestion's rate when the user holds nothing eligible; `null` when not applicable
     * @property potentialRewardType label of [potentialReward]'s rate kind (APR/APY); only set alongside
     * a rate-based reward
     */
    data class Content(
        override val tokenList: ImmutableList<ForYouWalletGroupUM>,
        @param:StringRes val subtitleRes: Int,
        val potentialReward: TextReference?,
        val potentialRewardType: TextReference?,
        val onAllEarnTokensClick: () -> Unit,
    ) : EarnOpportunitiesUM
}

/**
 * A wallet section of the token list: an optional [header] row (the wallet name + device icon,
 * modelled on the portfolio selector's `WalletNameRow`) followed by its [items]. [header] is `null`
 * when the list is rendered flat (a single wallet, or a section that isn't grouped by wallet), in
 * which case no header row is drawn.
 */
@Immutable
internal data class ForYouWalletGroupUM(
    val header: ForYouWalletHeaderUM?,
    val items: ImmutableList<ForYouTokenListItemUM>,
)

@Immutable
internal data class ForYouWalletHeaderUM(
    val id: String,
    val name: TextReference,
    val deviceIcon: DeviceIconUM,
)

@Immutable
internal data class ForYouTokenListItemUM(
    val tokenRowUM: TangemTokenRowUM,
    val tokenList: ImmutableList<TangemTokenRowUM>,
    val isExpanded: Boolean,
    val isExpandable: Boolean,
    val segmentColor: DonutSegmentColor?,
)

/**
 * Wraps a flat list of items in a single header-less group, for sections not grouped by wallet.

 * items" and "no groups" the same way.
 */
internal fun ImmutableList<ForYouTokenListItemUM>.asSingleForYouGroup(): ImmutableList<ForYouWalletGroupUM> =
    if (isEmpty()) persistentListOf() else persistentListOf(ForYouWalletGroupUM(header = null, items = this))