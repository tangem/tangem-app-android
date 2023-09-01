package com.tangem.feature.tokendetails.presentation.tokendetails.state

import com.tangem.core.ui.components.marketprice.MarketPriceBlockState
import com.tangem.core.ui.components.transactions.state.TxHistoryState
import com.tangem.feature.tokendetails.presentation.tokendetails.state.components.TokenDetailsDialogConfig

internal data class TokenDetailsState(
    val topAppBarConfig: TokenDetailsTopAppBarConfig,
    val tokenInfoBlockState: TokenInfoBlockState,
    val tokenBalanceBlockState: TokenDetailsBalanceBlockState,
    val marketPriceBlockState: MarketPriceBlockState,
    val txHistoryState: TxHistoryState,
    val dialogConfig: TokenDetailsDialogConfig?,
)