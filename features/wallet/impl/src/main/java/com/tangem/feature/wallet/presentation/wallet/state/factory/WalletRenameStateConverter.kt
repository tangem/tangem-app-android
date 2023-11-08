package com.tangem.feature.wallet.presentation.wallet.state.factory

import com.tangem.common.Provider
import com.tangem.feature.wallet.presentation.wallet.state.WalletState
import com.tangem.feature.wallet.presentation.wallet.state.components.WalletsListConfig
import com.tangem.utils.converter.Converter
import kotlinx.collections.immutable.toImmutableList

internal class WalletRenameStateConverter(
    private val currentStateProvider: Provider<WalletState>,
) : Converter<String, WalletState> {

    override fun convert(value: String): WalletState {
        return when (val state = currentStateProvider()) {
            is WalletState.ContentState -> {
                state.copySealed(
                    walletsListConfig = state.walletsListConfig.renameSelectedWallet(name = value),
                )
            }
            is WalletState.Initial -> state
        }
    }

    private fun WalletsListConfig.renameSelectedWallet(name: String): WalletsListConfig {
        return copy(
            wallets = wallets
                .mapIndexed { index, walletCard ->
                    if (index == selectedWalletIndex) walletCard.copySealed(title = name) else walletCard
                }
                .toImmutableList(),
        )
    }
}