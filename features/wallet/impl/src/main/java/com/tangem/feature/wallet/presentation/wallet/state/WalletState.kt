package com.tangem.feature.wallet.presentation.wallet.state

import com.tangem.core.ui.components.bottomsheets.TangemBottomSheetConfig
import com.tangem.core.ui.event.StateEvent
import com.tangem.feature.wallet.presentation.wallet.state.components.WalletNotification
import com.tangem.feature.wallet.presentation.wallet.state.components.WalletPullToRefreshConfig
import com.tangem.feature.wallet.presentation.wallet.state.components.WalletTopBarConfig
import com.tangem.feature.wallet.presentation.wallet.state.components.WalletsListConfig
import kotlinx.collections.immutable.ImmutableList

/**
 * Wallet screen state
 *
[REDACTED_AUTHOR]
 */
internal sealed class WalletState {

    /** Lambda be invoked when back button is clicked */
    abstract val onBackClick: () -> Unit

    /** Wallet screen content state */
    sealed class ContentState : WalletState() {

        /** Top bar config */
        abstract val topBarConfig: WalletTopBarConfig

        /** Wallets list config */
        abstract val walletsListConfig: WalletsListConfig

        /** Pull to refresh config */
        abstract val pullToRefreshConfig: WalletPullToRefreshConfig

        /** Notifications */
        abstract val notifications: ImmutableList<WalletNotification>

        /** Bottom sheet config */
        abstract val bottomSheetConfig: TangemBottomSheetConfig?

        /** State event */
        abstract val event: StateEvent<WalletEvent>

        /**
         * Util function that allow to make a copy
         *
         * @param walletsListConfig   wallets list config
         * @param pullToRefreshConfig pull to refresh config
         * @param event               state event
         */
        fun copySealed(
            walletsListConfig: WalletsListConfig = this.walletsListConfig,
            pullToRefreshConfig: WalletPullToRefreshConfig = this.pullToRefreshConfig,
            event: StateEvent<WalletEvent> = this.event,
        ): ContentState {
            return when (this) {
                is WalletMultiCurrencyState.Content -> {
                    copy(
                        walletsListConfig = walletsListConfig,
                        pullToRefreshConfig = pullToRefreshConfig,
                        event = event,
                    )
                }
                is WalletMultiCurrencyState.Locked -> {
                    copy(
                        walletsListConfig = walletsListConfig,
                        pullToRefreshConfig = pullToRefreshConfig,
                        event = event,
                    )
                }
                is WalletSingleCurrencyState.Content -> {
                    copy(
                        walletsListConfig = walletsListConfig,
                        pullToRefreshConfig = pullToRefreshConfig,
                        event = event,
                    )
                }
                is WalletSingleCurrencyState.Locked -> {
                    copy(
                        walletsListConfig = walletsListConfig,
                        pullToRefreshConfig = pullToRefreshConfig,
                        event = event,
                    )
                }
            }
        }
    }

    /**
     * Initial state
     *
     * @property onBackClick lambda be invoked when back button is clicked
     */
    data class Initial(override val onBackClick: () -> Unit) : WalletState()
}