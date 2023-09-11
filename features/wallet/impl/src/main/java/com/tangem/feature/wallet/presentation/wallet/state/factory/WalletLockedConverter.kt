package com.tangem.feature.wallet.presentation.wallet.state.factory

import com.tangem.common.Provider
import com.tangem.feature.wallet.presentation.wallet.state.WalletMultiCurrencyState
import com.tangem.feature.wallet.presentation.wallet.state.WalletSingleCurrencyState
import com.tangem.feature.wallet.presentation.wallet.state.WalletState
import com.tangem.feature.wallet.presentation.wallet.state.components.WalletManageButton
import com.tangem.feature.wallet.presentation.wallet.state.components.WalletPullToRefreshConfig
import com.tangem.feature.wallet.presentation.wallet.state.components.WalletTopBarConfig
import com.tangem.feature.wallet.presentation.wallet.viewmodels.WalletClickIntents
import com.tangem.utils.converter.Converter
import kotlinx.collections.immutable.PersistentList
import kotlinx.collections.immutable.toPersistentList

internal class WalletLockedConverter(
    private val currentStateProvider: Provider<WalletState>,
    private val clickIntents: WalletClickIntents,
) : Converter<Unit, WalletState> {

    override fun convert(value: Unit): WalletState {
        return when (val state = currentStateProvider()) {
            is WalletMultiCurrencyState.Content -> state.toMultiCurrencyLockedState()
            is WalletSingleCurrencyState.Content -> state.toSingleCurrencyLockedState()
            is WalletMultiCurrencyState.Locked,
            is WalletSingleCurrencyState.Locked,
            is WalletState.Initial,
            -> state
        }
    }

    private fun WalletMultiCurrencyState.Content.toMultiCurrencyLockedState(): WalletState {
        return WalletMultiCurrencyState.Locked(
            onBackClick = onBackClick,
            topBarConfig = topBarConfig.updateCallback(),
            walletsListConfig = walletsListConfig,
            pullToRefreshConfig = pullToRefreshConfig.stopRefreshing(),
            onUnlockWalletsNotificationClick = clickIntents::onUnlockWalletNotificationClick,
            onUnlockClick = clickIntents::onUnlockWalletClick,
            onScanClick = clickIntents::onScanToUnlockWalletClick,
        )
    }

    private fun WalletSingleCurrencyState.Content.toSingleCurrencyLockedState(): WalletState {
        return WalletSingleCurrencyState.Locked(
            onBackClick = onBackClick,
            topBarConfig = topBarConfig.updateCallback(),
            walletsListConfig = walletsListConfig,
            pullToRefreshConfig = pullToRefreshConfig.stopRefreshing(),
            buttons = buttons.disableButtons(),
            onUnlockWalletsNotificationClick = clickIntents::onUnlockWalletNotificationClick,
            onUnlockClick = clickIntents::onUnlockWalletClick,
            onScanClick = clickIntents::onScanToUnlockWalletClick,
            onExploreClick = clickIntents::onExploreClick,
        )
    }

    private fun WalletTopBarConfig.updateCallback(): WalletTopBarConfig {
        return copy(onMoreClick = clickIntents::onUnlockWalletNotificationClick)
    }

    private fun WalletPullToRefreshConfig.stopRefreshing(): WalletPullToRefreshConfig {
        return copy(isRefreshing = false)
    }

    private fun PersistentList<WalletManageButton>.disableButtons(): PersistentList<WalletManageButton> {
        return this
            .map { button ->
                when (button) {
                    is WalletManageButton.Buy -> button.copy(enabled = false)
                    is WalletManageButton.Sell -> button.copy(enabled = false)
                    is WalletManageButton.Send -> button.copy(enabled = false)
                    is WalletManageButton.Swap -> button.copy(enabled = false)
                    is WalletManageButton.Receive -> button
                }
            }
            .toPersistentList()
    }
}