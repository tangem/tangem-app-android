package com.tangem.feature.wallet.presentation.wallet.state2

import androidx.paging.PagingData
import com.tangem.core.ui.components.bottomsheets.TangemBottomSheetConfig
import com.tangem.core.ui.components.marketprice.MarketPriceBlockState
import com.tangem.core.ui.components.transactions.state.TransactionState
import com.tangem.core.ui.components.transactions.state.TxHistoryState
import com.tangem.core.ui.event.StateEvent
import com.tangem.core.ui.extensions.TextReference
import com.tangem.feature.wallet.impl.R
import com.tangem.feature.wallet.presentation.common.state.TokenItemState
import com.tangem.feature.wallet.presentation.wallet.state.WalletEvent
import com.tangem.feature.wallet.presentation.wallet.state.components.*
import kotlinx.collections.immutable.ImmutableList
import kotlinx.collections.immutable.PersistentList
import kotlinx.collections.immutable.persistentListOf
import kotlinx.coroutines.flow.MutableStateFlow
import javax.annotation.concurrent.Immutable

const val NOT_INITIALIZED_WALLET_INDEX = -1

internal data class WalletScreenState(
    val onBackClick: () -> Unit,
    val topBarConfig: WalletTopBarConfig,
    val selectedWalletIndex: Int,
    val wallets: ImmutableList<WalletState>,
    val onWalletChange: (Int) -> Unit,
    val event: StateEvent<WalletEvent>,
    val isHidingMode: Boolean,
)

internal sealed class WalletState {

    abstract val pullToRefreshConfig: WalletPullToRefreshConfig
    abstract val walletCardState: WalletCardState
    abstract val warnings: ImmutableList<WalletNotification>
    abstract val bottomSheetConfig: TangemBottomSheetConfig?

    sealed class MultiCurrency : WalletState() {

        abstract val tokensListState: WalletTokensListState
        abstract val manageTokensButtonConfig: ManageTokensButtonConfig?

        data class Content(
            override val pullToRefreshConfig: WalletPullToRefreshConfig,
            override val walletCardState: WalletCardState,
            override val warnings: ImmutableList<WalletNotification>,
            override val bottomSheetConfig: TangemBottomSheetConfig?,
            override val tokensListState: WalletTokensListState,
            override val manageTokensButtonConfig: ManageTokensButtonConfig?,
        ) : MultiCurrency()

        data class Locked(
            override val walletCardState: WalletCardState,
            val onUnlockNotificationClick: () -> Unit,
            val isBottomSheetShow: Boolean = false,
            val onBottomSheetDismiss: () -> Unit = {},
            val onUnlockClick: () -> Unit,
            val onScanClick: () -> Unit,
        ) : MultiCurrency() {

            override val pullToRefreshConfig: WalletPullToRefreshConfig
                get() = WalletPullToRefreshConfig(isRefreshing = false, onRefresh = {})

            override val warnings: ImmutableList<WalletNotification> = persistentListOf(
                WalletNotification.UnlockWallets(onUnlockNotificationClick),
            )

            override val bottomSheetConfig = TangemBottomSheetConfig(
                isShow = isBottomSheetShow,
                onDismissRequest = onBottomSheetDismiss,
                content = WalletBottomSheetConfig.UnlockWallets(
                    onUnlockClick = onUnlockClick,
                    onScanClick = onScanClick,
                ),
            )

            override val tokensListState = WalletTokensListState.ContentState.Locked
            override val manageTokensButtonConfig = null
        }
    }

    sealed class SingleCurrency : WalletState() {

        abstract val buttons: PersistentList<WalletManageButton>
        abstract val marketPriceBlockState: MarketPriceBlockState?
        abstract val txHistoryState: TxHistoryState

        data class Content(
            override val pullToRefreshConfig: WalletPullToRefreshConfig,
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
            val onUnlockNotificationClick: () -> Unit,
            val isBottomSheetShow: Boolean = false,
            val onBottomSheetDismiss: () -> Unit = {},
            val onUnlockClick: () -> Unit,
            val onScanClick: () -> Unit,
            val onExploreClick: () -> Unit,
        ) : SingleCurrency() {

            override val pullToRefreshConfig: WalletPullToRefreshConfig
                get() = WalletPullToRefreshConfig(isRefreshing = false, onRefresh = {})

            override val warnings: ImmutableList<WalletNotification> = persistentListOf(
                WalletNotification.UnlockWallets(onUnlockNotificationClick),
            )

            override val bottomSheetConfig = TangemBottomSheetConfig(
                isShow = isBottomSheetShow,
                onDismissRequest = onBottomSheetDismiss,
                content = WalletBottomSheetConfig.UnlockWallets(
                    onUnlockClick = onUnlockClick,
                    onScanClick = onScanClick,
                ),
            )

            override val marketPriceBlockState: MarketPriceBlockState? = null

            override val txHistoryState: TxHistoryState = TxHistoryState.Content(
                contentItems = MutableStateFlow(
                    value = PagingData.from(
                        data = listOf(
                            TxHistoryState.TxHistoryItemState.Title(onExploreClick = onExploreClick),
                            TxHistoryState.TxHistoryItemState.Transaction(
                                state = TransactionState.Locked(txHash = "LOCKED_TX_HASH"),
                            ),
                        ),
                    ),
                ),
            )
        }
    }

    internal sealed class Visa : WalletState() {

        abstract val balancesAndLimitBlockState: BalancesAndLimitsBlockState?
        abstract val txHistoryState: TxHistoryState

        data class Content(
            override val pullToRefreshConfig: WalletPullToRefreshConfig,
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
            val isBottomSheetShow: Boolean = false,
            val onBottomSheetDismiss: () -> Unit = {},
            val onUnlockClick: () -> Unit,
            val onScanClick: () -> Unit,
            val onExploreClick: () -> Unit,
        ) : Visa() {

            override val pullToRefreshConfig: WalletPullToRefreshConfig
                get() = WalletPullToRefreshConfig(isRefreshing = false, onRefresh = {})

            override val warnings: ImmutableList<WalletNotification> = persistentListOf(
                WalletNotification.UnlockWallets(onUnlockNotificationClick),
            )

            override val bottomSheetConfig = TangemBottomSheetConfig(
                isShow = isBottomSheetShow,
                onDismissRequest = onBottomSheetDismiss,
                content = WalletBottomSheetConfig.UnlockWallets(
                    onUnlockClick = onUnlockClick,
                    onScanClick = onScanClick,
                ),
            )

            override val balancesAndLimitBlockState: BalancesAndLimitsBlockState? = null

            override val txHistoryState: TxHistoryState = TxHistoryState.Content(
                contentItems = MutableStateFlow(
                    value = PagingData.from(
                        data = listOf(
                            TxHistoryState.TxHistoryItemState.Title(onExploreClick = onExploreClick),
                            TxHistoryState.TxHistoryItemState.Transaction(
                                state = TransactionState.Locked(txHash = "LOCKED_TX_HASH"),
                            ),
                        ),
                    ),
                ),
            )
        }

        sealed class BalancesAndLimitsBlockState {

            object Loading : BalancesAndLimitsBlockState()

            object Error : BalancesAndLimitsBlockState()

            data class Content(
                val availableBalance: String,
                val currencySymbol: String,
                val limitDays: Int,
                val isEnabled: Boolean,
                val onClick: () -> Unit,
            ) : BalancesAndLimitsBlockState()
        }

        data class DepositButtonState(
            val isEnabled: Boolean,
            val onClick: () -> Unit,
        )
    }
}

internal sealed class WalletTokensListState {

    object Empty : WalletTokensListState()

    sealed class ContentState : WalletTokensListState() {

        abstract val items: ImmutableList<TokensListItemState>
        abstract val organizeTokensButtonConfig: OrganizeTokensButtonConfig?

        object Loading : ContentState() {
            override val items = persistentListOf<TokensListItemState>()
            override val organizeTokensButtonConfig = null
        }

        data class Content(
            override val items: ImmutableList<TokensListItemState>,
            override val organizeTokensButtonConfig: OrganizeTokensButtonConfig?,
        ) : ContentState()

        object Locked : ContentState() {
            override val items = persistentListOf(
                TokensListItemState.NetworkGroupTitle(id = 42, name = TextReference.Res(id = R.string.main_tokens)),
                TokensListItemState.Token(state = TokenItemState.Locked(id = "Locked#1")),
            )
            override val organizeTokensButtonConfig = null
        }
    }

    data class OrganizeTokensButtonConfig(val isEnabled: Boolean, val onClick: () -> Unit)

    @Immutable
    sealed class TokensListItemState {

        abstract val id: Any

        data class NetworkGroupTitle(override val id: Int, val name: TextReference) : TokensListItemState()

        data class Token(val state: TokenItemState) : TokensListItemState() {
            override val id: String = state.id
        }
    }
}

internal data class ManageTokensButtonConfig(val onClick: () -> Unit)