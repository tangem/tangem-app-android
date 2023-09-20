package com.tangem.feature.wallet.presentation.wallet.utils

import com.tangem.common.Converter
import com.tangem.common.Provider
import com.tangem.feature.wallet.presentation.common.state.TokenItemState
import com.tangem.feature.wallet.presentation.organizetokens.utils.converter.TokenItemHiddenStateConverter
import com.tangem.feature.wallet.presentation.wallet.state.WalletMultiCurrencyState
import com.tangem.feature.wallet.presentation.wallet.state.WalletSingleCurrencyState
import com.tangem.feature.wallet.presentation.wallet.state.WalletState
import com.tangem.feature.wallet.presentation.wallet.state.components.WalletTokensListState
import kotlinx.collections.immutable.toImmutableList

internal class HiddenStateConverter(
    private val currentStateProvider: Provider<WalletState>,
) : Converter<Boolean, WalletState> {

    private val walletHiddenBalanceStateConverter by lazy { WalletHiddenBalanceStateConverter() }

    private val tokenItemHiddenStateConverter by lazy { TokenItemHiddenStateConverter() }

    override fun convert(value: Boolean): WalletState {
        return when (val state = currentStateProvider() as? WalletState.ContentState) {
            is WalletMultiCurrencyState.Content -> {
                val updatedTokensList = (state.tokensListState as? WalletTokensListState.Content)?.let { content ->
                    content.copy(
                        items = content.items.map { tokenListItemState ->
                            if (tokenListItemState is WalletTokensListState.TokensListItemState.Token) {
                                if (tokenListItemState.state is TokenItemState.Content) {
                                    tokenListItemState.copy(
                                        state = tokenListItemState.state.copy(
                                            tokenOptions = tokenItemHiddenStateConverter.updateHiddenState(
                                                optionsState = tokenListItemState.state.tokenOptions,
                                                isBalanceHidden = value,
                                            ),
                                        ),
                                    )
                                } else {
                                    tokenListItemState
                                }
                            } else {
                                tokenListItemState
                            }
                        }.toImmutableList(),
                    )
                } ?: state.tokensListState

                state.copy(
                    walletsListConfig = state.walletsListConfig.copy(
                        wallets = state.walletsListConfig.wallets.map {
                            walletHiddenBalanceStateConverter.updateHiddenState(it, value)
                        }.toImmutableList(),
                    ),
                    tokensListState = updatedTokensList,
                )
            }

            is WalletSingleCurrencyState.Content -> {
                state.copy(
                    walletsListConfig = state.walletsListConfig.copy(
                        wallets = state.walletsListConfig.wallets.map {
                            walletHiddenBalanceStateConverter.updateHiddenState(it, value)
                        }.toImmutableList(),
                    ),
                )
            }

            else -> currentStateProvider()
        }
    }
}