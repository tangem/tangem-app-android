package com.tangem.feature.wallet.presentation.wallet.state.model

import androidx.compose.runtime.Immutable
import com.tangem.common.ui.expressStatus.state.ExpressTransactionStateUM
import com.tangem.core.ui.components.bottomsheets.TangemBottomSheetConfig
import com.tangem.core.ui.components.containers.pullToRefresh.PullToRefreshConfig
import com.tangem.core.ui.components.marketprice.MarketPriceBlockState
import com.tangem.core.ui.components.transactions.state.TxHistoryState
import com.tangem.feature.wallet.presentation.wallet.state.model.holder.LockedTxHistoryStateHolder
import com.tangem.feature.wallet.presentation.wallet.state.model.holder.LockedWalletStateHolder
import com.tangem.feature.wallet.presentation.wallet.state.model.holder.TxHistoryStateHolder
import com.tangem.feature.wallet.presentation.wallet.state.model.holder.WalletStateHolder
import kotlinx.collections.immutable.ImmutableList
import kotlinx.collections.immutable.PersistentList

internal const val NOT_INITIALIZED_WALLET_INDEX = -1

@Immutable
internal sealed interface WalletState : WalletStateHolder {

    sealed class MultiCurrency : WalletState {

        abstract val tokensListState: WalletTokensListState
        abstract val nftState: WalletNFTItemUM
        abstract val type: WalletType
        abstract val tangemPayState: TangemPayState

        data class Content(
            override val pullToRefreshConfig: PullToRefreshConfig,
            override val walletCardState: WalletCardState,
            override val buttons: PersistentList<WalletManageButton>,
            override val warnings: ImmutableList<WalletNotification>,
            override val bottomSheetConfig: TangemBottomSheetConfig?,
            override val tokensListState: WalletTokensListState,
            override val nftState: WalletNFTItemUM,
            override val type: WalletType,
            override val tangemPayState: TangemPayState,
        ) : MultiCurrency()

        data class Locked(
            override val walletCardState: WalletCardState,
            override val buttons: PersistentList<WalletManageButton>,
            override val bottomSheetConfig: TangemBottomSheetConfig?,
            override val type: WalletType,
            val onUnlockNotificationClick: () -> Unit,
        ) : MultiCurrency(),
            WalletStateHolder by LockedWalletStateHolder(
                walletCardState = walletCardState,
                buttons = buttons,
                bottomSheetConfig = bottomSheetConfig,
                onUnlockNotificationClick = onUnlockNotificationClick,
            ) {

            override val tokensListState = WalletTokensListState.ContentState.Locked
            override val nftState: WalletNFTItemUM = WalletNFTItemUM.Hidden
            override val tangemPayState: TangemPayState = TangemPayState.Empty
        }

        enum class WalletType {
            Hot,
            Cold,
        }
    }

    sealed class SingleCurrency : WalletState, TxHistoryStateHolder {

        abstract val marketPriceBlockState: MarketPriceBlockState?

        data class Content(
            override val pullToRefreshConfig: PullToRefreshConfig,
            override val walletCardState: WalletCardState,
            override val warnings: ImmutableList<WalletNotification>,
            override val bottomSheetConfig: TangemBottomSheetConfig?,
            override val buttons: PersistentList<WalletManageButton>,
            override val marketPriceBlockState: MarketPriceBlockState,
            override val txHistoryState: TxHistoryState,
            val expressTxsToDisplay: PersistentList<ExpressTransactionStateUM>,
            val expressTxs: PersistentList<ExpressTransactionStateUM>,
        ) : SingleCurrency()

        data class Locked(
            override val walletCardState: WalletCardState,
            override val buttons: PersistentList<WalletManageButton>,
            override val bottomSheetConfig: TangemBottomSheetConfig?,
            val onUnlockNotificationClick: () -> Unit,
            val onExploreClick: () -> Unit,
        ) : SingleCurrency(),
            TxHistoryStateHolder by LockedTxHistoryStateHolder(onExploreClick),
            WalletStateHolder by LockedWalletStateHolder(
                walletCardState = walletCardState,
                buttons = buttons,
                bottomSheetConfig = bottomSheetConfig,
                onUnlockNotificationClick = onUnlockNotificationClick,
            ) {

            override val marketPriceBlockState: MarketPriceBlockState? = null
        }
    }
}