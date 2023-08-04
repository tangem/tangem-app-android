package com.tangem.feature.tokendetails.presentation.tokendetails.state

import com.tangem.core.ui.components.marketprice.MarketPriceBlockState

data class TokenDetailsState(
    val topAppBarConfig: TokenDetailsTopAppBarConfig,
    val tokenInfoBlockState: TokenInfoBlockState,
    val tokenBalanceBlockState: TokenDetailsBalanceBlockState,
    val marketPriceBlockState: MarketPriceBlockState,
)
