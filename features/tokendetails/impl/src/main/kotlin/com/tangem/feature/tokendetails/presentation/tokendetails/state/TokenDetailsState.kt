package com.tangem.feature.tokendetails.presentation.tokendetails.state

import com.tangem.core.ui.components.containers.pullToRefresh.PullToRefreshConfig
import com.tangem.core.ui.components.marketprice.MarketPriceBlockState
import com.tangem.feature.tokendetails.presentation.tokendetails.state.components.TokenDetailsNotification
import kotlinx.collections.immutable.ImmutableList

internal data class TokenDetailsState(
    val topAppBarConfig: TokenDetailsTopAppBarConfig,
    val tokenInfoBlockState: TokenInfoBlockState,
    val tokenBalanceBlockState: TokenDetailsBalanceBlockState,
    val marketPriceBlockState: MarketPriceBlockState,
    val stakingBlocksState: StakingBlockUM?,
    val notifications: ImmutableList<TokenDetailsNotification>,
    val pullToRefreshConfig: PullToRefreshConfig,
    val isBalanceHidden: Boolean,
    val isMarketPriceAvailable: Boolean,
)