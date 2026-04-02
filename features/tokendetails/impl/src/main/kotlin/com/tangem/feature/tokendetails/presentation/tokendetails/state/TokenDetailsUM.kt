package com.tangem.feature.tokendetails.presentation.tokendetails.state

import androidx.compose.runtime.Stable
import com.tangem.core.ui.components.containers.pullToRefresh.PullToRefreshConfig
import com.tangem.core.ui.components.marketprice.MarketPriceBlockState
import com.tangem.core.ui.extensions.TextReference
import kotlinx.collections.immutable.ImmutableList

@Stable
internal data class TokenDetailsUM(
    val topAppBarUM: TokenDetailsTopAppBarUM,
    val balanceBlockUM: TokenDetailsBalanceBlockUM,
    val marketPriceBlockState: MarketPriceBlockState,
    val stakingBlocksState: StakingBlockUM?,
    val pullToRefreshConfig: PullToRefreshConfig,
    val isBalanceHidden: Boolean,
    val isMarketPriceAvailable: Boolean,
)

internal data class TokenDetailsTopAppBarUM(
    val title: TextReference,
    val subtitle: TextReference,
    val menuItems: ImmutableList<TextReference>,
)