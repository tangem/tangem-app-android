package com.tangem.feature.wallet.presentation.wallet.state.factory

import com.tangem.common.Provider
import com.tangem.feature.wallet.presentation.wallet.state.WalletMultiCurrencyState
import com.tangem.feature.wallet.presentation.wallet.state.WalletSingleCurrencyState
import com.tangem.feature.wallet.presentation.wallet.state.WalletState
import com.tangem.feature.wallet.presentation.wallet.state.components.WalletManageButton
import com.tangem.feature.wallet.presentation.wallet.state.components.WalletPullToRefreshConfig
import com.tangem.feature.wallet.presentation.wallet.state.components.WalletTokensListState
import com.tangem.utils.converter.Converter
import kotlinx.collections.immutable.PersistentList
import kotlinx.collections.immutable.mutate

internal class WalletRefreshStateConverter(
    private val currentStateProvider: Provider<WalletState>,
) : Converter<Boolean, WalletState> {

    override fun convert(value: Boolean): WalletState {
        val state = currentStateProvider()
        val contentState = state as? WalletState.ContentState ?: return state

        return if (value) {
            contentState.getRefreshingState()
        } else {
            contentState.getRefreshedState()
        }
    }

    private fun WalletState.ContentState.getRefreshingState(): WalletState {
        return when (this) {
            is WalletMultiCurrencyState.Content -> getRefreshingState()
            is WalletSingleCurrencyState.Content -> getRefreshingState()
            is WalletMultiCurrencyState.Locked,
            is WalletSingleCurrencyState.Locked,
            -> this
        }
    }

    private fun WalletState.ContentState.getRefreshedState(): WalletState {
        return when (this) {
            is WalletMultiCurrencyState.Content -> getRefreshedState()
            is WalletSingleCurrencyState.Content -> getRefreshedState()
            is WalletMultiCurrencyState.Locked,
            is WalletSingleCurrencyState.Locked,
            -> this
        }
    }

    private fun WalletMultiCurrencyState.Content.getRefreshingState(): WalletMultiCurrencyState {
        return copy(
            pullToRefreshConfig = updatePullToRefreshConfig(isRefreshing = true),
            tokensListState = updateTokenListState(isRefreshing = true),
        )
    }

    private fun WalletSingleCurrencyState.Content.getRefreshingState(): WalletSingleCurrencyState {
        return copy(
            pullToRefreshConfig = updatePullToRefreshConfig(isRefreshing = true),
            buttons = updateButtons(isRefreshing = true),
        )
    }

    private fun WalletMultiCurrencyState.Content.getRefreshedState(): WalletMultiCurrencyState {
        return copy(
            pullToRefreshConfig = updatePullToRefreshConfig(isRefreshing = false),
            tokensListState = updateTokenListState(isRefreshing = false),
        )
    }

    private fun WalletSingleCurrencyState.Content.getRefreshedState(): WalletSingleCurrencyState {
        return copy(
            pullToRefreshConfig = updatePullToRefreshConfig(isRefreshing = false),
            buttons = updateButtons(isRefreshing = false),
        )
    }

    private fun WalletMultiCurrencyState.updateTokenListState(isRefreshing: Boolean): WalletTokensListState {
        return when (val listState = tokensListState) {
            is WalletTokensListState.Content -> {
                when (listState.organizeTokensButton) {
                    is WalletTokensListState.OrganizeTokensButtonState.Hidden -> listState
                    is WalletTokensListState.OrganizeTokensButtonState.Visible -> listState.copy(
                        organizeTokensButton = listState.organizeTokensButton.copy(
                            isEnabled = !isRefreshing,
                        ),
                    )
                }
            }
            is WalletTokensListState.Locked,
            is WalletTokensListState.Loading,
            is WalletTokensListState.Empty,
            -> listState
        }
    }

    private fun WalletSingleCurrencyState.updateButtons(isRefreshing: Boolean): PersistentList<WalletManageButton> {
        val isButtonsEnabled = !isRefreshing

        return buttons.mutate {
            it.mapNotNull { button ->
                when (button) {
                    is WalletManageButton.Buy -> button.copy(enabled = isButtonsEnabled)
                    is WalletManageButton.Send -> button.copy(enabled = isButtonsEnabled)
                    is WalletManageButton.Sell -> button.copy(enabled = isButtonsEnabled)
                    is WalletManageButton.Receive -> button
                    is WalletManageButton.Swap -> null
                }
            }
        }
    }

    private fun WalletState.ContentState.updatePullToRefreshConfig(isRefreshing: Boolean): WalletPullToRefreshConfig {
        return pullToRefreshConfig.copy(isRefreshing = isRefreshing)
    }
}