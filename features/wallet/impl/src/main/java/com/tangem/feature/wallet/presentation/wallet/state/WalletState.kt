package com.tangem.feature.wallet.presentation.wallet.state

import com.tangem.feature.wallet.presentation.wallet.state.components.*
import kotlinx.collections.immutable.ImmutableList

/**
 * Wallet screen state
 *
* [REDACTED_AUTHOR]
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
        abstract val bottomSheetConfig: WalletBottomSheetConfig?

        /**
         * Util function that allow to make a copy
         *
         * @param walletsListConfig wallets list config
         */
        fun copySealed(walletsListConfig: WalletsListConfig = this.walletsListConfig): ContentState {
            return when (this) {
                is WalletMultiCurrencyState.Content -> copy(walletsListConfig = walletsListConfig)
                is WalletMultiCurrencyState.Locked -> copy(walletsListConfig = walletsListConfig)
                is WalletSingleCurrencyState.Content -> copy(walletsListConfig = walletsListConfig)
                is WalletSingleCurrencyState.Locked -> copy(walletsListConfig = walletsListConfig)
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
