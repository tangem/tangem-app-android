package com.tangem.feature.wallet.presentation.wallet.state

import androidx.compose.runtime.Immutable
import androidx.paging.PagingData
import com.tangem.core.ui.components.marketprice.MarketPriceBlockState
import com.tangem.core.ui.components.transactions.state.TransactionState
import com.tangem.core.ui.components.transactions.state.TxHistoryState
import com.tangem.feature.wallet.presentation.wallet.state.components.*
import kotlinx.collections.immutable.ImmutableList
import kotlinx.collections.immutable.PersistentList
import kotlinx.collections.immutable.persistentListOf
import kotlinx.coroutines.flow.MutableStateFlow

/**
 * Single currency wallet content state
 *
[REDACTED_AUTHOR]
 */
@Immutable
internal sealed class WalletSingleCurrencyState : WalletState.ContentState() {

    /** Manage buttons */
    abstract val buttons: PersistentList<WalletManageButton>

    /** Transactions history state */
    abstract val txHistoryState: TxHistoryState

    data class Content(
        override val onBackClick: () -> Unit,
        override val topBarConfig: WalletTopBarConfig,
        override val walletsListConfig: WalletsListConfig,
        override val pullToRefreshConfig: WalletPullToRefreshConfig,
        override val notifications: ImmutableList<WalletNotification>,
        override val bottomSheetConfig: WalletBottomSheetConfig?,
        override val buttons: PersistentList<WalletManageButton>,
        override val txHistoryState: TxHistoryState,
        val marketPriceBlockState: MarketPriceBlockState,
    ) : WalletSingleCurrencyState()

    data class Locked(
        override val onBackClick: () -> Unit,
        override val topBarConfig: WalletTopBarConfig,
        override val walletsListConfig: WalletsListConfig,
        override val pullToRefreshConfig: WalletPullToRefreshConfig,
        override val buttons: PersistentList<WalletManageButton>,
        override val onUnlockWalletsNotificationClick: () -> Unit,
        override val onUnlockClick: () -> Unit,
        override val onScanClick: () -> Unit,
        override val isBottomSheetShow: Boolean = false,
        override val onBottomSheetDismiss: () -> Unit = {},
        val onExploreClick: () -> Unit,
    ) : WalletSingleCurrencyState(), WalletLockedState {

        override val notifications = persistentListOf(
            WalletNotification.UnlockWallets(onUnlockWalletsNotificationClick),
        )

        override val bottomSheetConfig = WalletBottomSheetConfig(
            isShow = isBottomSheetShow,
            onDismissRequest = onBottomSheetDismiss,
            content = WalletBottomSheetConfig.BottomSheetContentConfig.UnlockWallets(
                onUnlockClick = onUnlockClick,
                onScanClick = onScanClick,
            ),
        )

        override val txHistoryState: TxHistoryState = TxHistoryState.Content(
            contentItems = MutableStateFlow(
                value = PagingData.from(
                    data = listOf(
                        TxHistoryState.TxHistoryItemState.Title(onExploreClick = onExploreClick),
                        TxHistoryState.TxHistoryItemState.Transaction(
                            state = TransactionState.Locked(txHash = LOCKED_TX_HASH),
                        ),
                    ),
                ),
            ),
        )

        private companion object {
            const val LOCKED_TX_HASH = "LOCKED_TX_HASH"
        }
    }
}