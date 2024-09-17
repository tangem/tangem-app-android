package com.tangem.feature.wallet.presentation.wallet.state.model

import androidx.compose.runtime.Immutable
import com.tangem.core.ui.components.bottomsheets.TangemBottomSheetConfig
import com.tangem.core.ui.components.marketprice.MarketPriceBlockState
import com.tangem.core.ui.components.transactions.state.TxHistoryState
import com.tangem.core.ui.pullToRefresh.PullToRefreshConfig
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
        abstract val manageTokensButtonConfig: ManageTokensButtonConfig?

        data class Content(
            override val pullToRefreshConfig: PullToRefreshConfig,
            override val walletCardState: WalletCardState,
            override val warnings: ImmutableList<WalletNotification>,
            override val bottomSheetConfig: TangemBottomSheetConfig?,
            override val tokensListState: WalletTokensListState,
            override val manageTokensButtonConfig: ManageTokensButtonConfig?,
        ) : MultiCurrency()

        data class Locked(
            override val walletCardState: WalletCardState,
            override val bottomSheetConfig: TangemBottomSheetConfig?,
            val onUnlockNotificationClick: () -> Unit,
        ) : MultiCurrency(),
            WalletStateHolder by LockedWalletStateHolder(
                walletCardState,
                bottomSheetConfig,
                onUnlockNotificationClick,
            ) {

            override val tokensListState = WalletTokensListState.ContentState.Locked
            override val manageTokensButtonConfig = null
        }
    }

    sealed class SingleCurrency : WalletState, TxHistoryStateHolder {

        abstract val buttons: PersistentList<WalletManageButton>
        abstract val marketPriceBlockState: MarketPriceBlockState?

        data class Content(
            override val pullToRefreshConfig: PullToRefreshConfig,
            override val walletCardState: WalletCardState,
            override val warnings: ImmutableList<WalletNotification>,
            override val bottomSheetConfig: TangemBottomSheetConfig?,
            override val buttons: PersistentList<WalletManageButton>,
            override val marketPriceBlockState: MarketPriceBlockState,
            override val txHistoryState: TxHistoryState,
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
                walletCardState,
                bottomSheetConfig,
                onUnlockNotificationClick,
            ) {

            override val marketPriceBlockState: MarketPriceBlockState? = null
        }
    }

    sealed class Visa : WalletState, TxHistoryStateHolder {

        abstract val balancesAndLimitBlockState: BalancesAndLimitsBlockState?

        data class Content(
            override val pullToRefreshConfig: PullToRefreshConfig,
            override val walletCardState: WalletCardState,
            override val warnings: ImmutableList<WalletNotification>,
            override val bottomSheetConfig: TangemBottomSheetConfig?,
            override val balancesAndLimitBlockState: BalancesAndLimitsBlockState,
            override val txHistoryState: TxHistoryState,
            val depositButtonState: DepositButtonState,
        ) : Visa()

        data class Locked(
            override val walletCardState: WalletCardState,
            val onUnlockNotificationClick: () -> Unit,
            override val bottomSheetConfig: TangemBottomSheetConfig?,
            val onExploreClick: () -> Unit,
        ) : Visa(),
            TxHistoryStateHolder by LockedTxHistoryStateHolder(onExploreClick),
            WalletStateHolder by LockedWalletStateHolder(
                walletCardState,
                bottomSheetConfig,
                onUnlockNotificationClick,
            ) {

            override val balancesAndLimitBlockState: BalancesAndLimitsBlockState? = null
        }
    }
}
