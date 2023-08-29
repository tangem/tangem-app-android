package com.tangem.feature.wallet.presentation.wallet.state.factory

import com.tangem.common.Provider
import com.tangem.feature.wallet.presentation.wallet.state.WalletMultiCurrencyState
import com.tangem.feature.wallet.presentation.wallet.state.WalletSingleCurrencyState
import com.tangem.feature.wallet.presentation.wallet.state.WalletState
import com.tangem.feature.wallet.presentation.wallet.state.components.WalletManageButton
import com.tangem.feature.wallet.presentation.wallet.state.components.WalletPullToRefreshConfig
import com.tangem.utils.converter.Converter
import kotlinx.collections.immutable.PersistentList
import kotlinx.collections.immutable.mutate

internal class WalletRefreshStateConverter(
    private val currentStateProvider: Provider<WalletState>,
) : Converter<Boolean, WalletState> {

    override fun convert(value: Boolean): WalletState {
        return if (value) {
            getRefreshingState()
        } else {
            getRefreshedState()
        }
    }

    private fun getRefreshedState(): WalletState {
        return when (val state = currentStateProvider()) {
            is WalletMultiCurrencyState.Content -> state.getRefreshedState()
            is WalletSingleCurrencyState.Content -> state.getRefreshedState()
            else -> state
        }
    }

    private fun getRefreshingState(): WalletState {
        return when (val state = currentStateProvider()) {
            is WalletMultiCurrencyState.Content -> state.getRefreshingState()
            is WalletSingleCurrencyState.Content -> state.getRefreshingState()
            else -> state
        }
    }

    private fun WalletMultiCurrencyState.Content.getRefreshedState(): WalletMultiCurrencyState.Content {
        return copy(
            pullToRefreshConfig = createPullToRefreshConfig(isRefreshing = false),
        )
    }

    private fun WalletSingleCurrencyState.Content.getRefreshedState(): WalletSingleCurrencyState.Content {
        return copy(
            pullToRefreshConfig = createPullToRefreshConfig(isRefreshing = false),
            buttons = buttons.mapToEnabledButtons(),
        )
    }

    private fun WalletMultiCurrencyState.Content.getRefreshingState(): WalletMultiCurrencyState.Content {
        return copy(
            pullToRefreshConfig = createPullToRefreshConfig(isRefreshing = true),
        )
    }

    private fun WalletSingleCurrencyState.Content.getRefreshingState(): WalletSingleCurrencyState.Content {
        return copy(
            pullToRefreshConfig = createPullToRefreshConfig(isRefreshing = true),
            buttons = buttons.mapToDisabledButton(),
        )
    }

    private fun WalletState.ContentState.createPullToRefreshConfig(isRefreshing: Boolean): WalletPullToRefreshConfig {
        return pullToRefreshConfig.copy(isRefreshing = isRefreshing)
    }

    private fun PersistentList<WalletManageButton>.mapToDisabledButton(): PersistentList<WalletManageButton> {
        return this.mutate {
            it.mapNotNull { button ->
                when (button) {
                    is WalletManageButton.Buy -> button.copy(enabled = false)
                    is WalletManageButton.Send -> button.copy(enabled = false)
                    is WalletManageButton.Receive -> button
                    is WalletManageButton.Sell -> button.copy(enabled = false)
                    is WalletManageButton.Swap -> null
                }
            }
        }
    }

    private fun PersistentList<WalletManageButton>.mapToEnabledButtons(): PersistentList<WalletManageButton> {
        return this.mutate {
            it.mapNotNull { button ->
                when (button) {
                    is WalletManageButton.Buy -> button.copy(enabled = true)
                    is WalletManageButton.Send -> button.copy(enabled = true)
                    is WalletManageButton.Receive -> button
                    is WalletManageButton.Sell -> button.copy(enabled = true)
                    is WalletManageButton.Swap -> null
                }
            }
        }
    }
}
