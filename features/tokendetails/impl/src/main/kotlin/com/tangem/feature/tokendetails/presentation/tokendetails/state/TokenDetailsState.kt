package com.tangem.feature.tokendetails.presentation.tokendetails.state

import com.tangem.core.ui.components.bottomsheets.TangemBottomSheetConfig
import com.tangem.core.ui.components.marketprice.MarketPriceBlockState
import com.tangem.core.ui.components.transactions.state.TransactionState
import com.tangem.core.ui.components.transactions.state.TxHistoryState
import com.tangem.core.ui.event.StateEvent
import com.tangem.core.ui.extensions.TextReference
import com.tangem.feature.tokendetails.presentation.tokendetails.state.components.TokenDetailsDialogConfig
import com.tangem.feature.tokendetails.presentation.tokendetails.state.components.TokenDetailsNotification
import com.tangem.feature.tokendetails.presentation.tokendetails.state.components.TokenDetailsPullToRefreshConfig
import kotlinx.collections.immutable.ImmutableList
import kotlinx.collections.immutable.PersistentList

internal data class TokenDetailsState(
    val topAppBarConfig: TokenDetailsTopAppBarConfig,
    val tokenInfoBlockState: TokenInfoBlockState,
    val tokenBalanceBlockState: TokenDetailsBalanceBlockState,
    val marketPriceBlockState: MarketPriceBlockState,
    val stakingBlocksState: StakingBlockUM,
    val notifications: ImmutableList<TokenDetailsNotification>,
    val pendingTxs: PersistentList<TransactionState>,
    val swapTxs: PersistentList<SwapTransactionsState>,
    val txHistoryState: TxHistoryState,
    val dialogConfig: TokenDetailsDialogConfig?,
    val pullToRefreshConfig: TokenDetailsPullToRefreshConfig,
    val bottomSheetConfig: TangemBottomSheetConfig?,
    val isBalanceHidden: Boolean,
    val isMarketPriceAvailable: Boolean,
    val isStakingBlockShown: Boolean,
    val event: StateEvent<TextReference>,
)
