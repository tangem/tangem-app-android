package com.tangem.feature.wallet.presentation.wallet.state.factory

import com.tangem.common.Provider
import com.tangem.domain.wallets.models.UserWalletId
import com.tangem.feature.wallet.presentation.wallet.state.WalletState
import com.tangem.feature.wallet.presentation.wallet.state.components.WalletCardState
import com.tangem.feature.wallet.presentation.wallet.state.factory.WalletDeleteStateConverter.DeleteWalletModel
import com.tangem.feature.wallet.presentation.wallet.viewmodels.WalletsUpdateActionResolver
import com.tangem.utils.converter.Converter
import kotlinx.collections.immutable.ImmutableList
import kotlinx.collections.immutable.toImmutableList

/**
 * Converter that responds on wallet deleting action. Returns [WalletState] without deleted wallet.
 *
 * @property currentStateProvider current state provider
 */
internal class WalletDeleteStateConverter(
    private val currentStateProvider: Provider<WalletState>,
) : Converter<DeleteWalletModel, WalletState> {

    override fun convert(value: DeleteWalletModel): WalletState {
        return when (val state = currentStateProvider()) {
            is WalletState.ContentState -> {
                value.cacheState.copySealed(
                    walletsListConfig = state.walletsListConfig.copy(
                        selectedWalletIndex = value.action.selectedWalletIndex,
                        wallets = state.walletsListConfig.wallets.deleteWallet(id = value.action.deletedWalletId),
                    ),
                    pullToRefreshConfig = value.cacheState.pullToRefreshConfig.copy(isRefreshing = false),
                )
            }
            is WalletState.Initial -> state
        }
    }

    private fun List<WalletCardState>.deleteWallet(id: UserWalletId): ImmutableList<WalletCardState> {
        return this
            .mapIndexedNotNull { index, currentWallet ->
                if (currentWallet.id == id) return@mapIndexedNotNull null
                getOrNull(index) ?: return@mapIndexedNotNull null
            }
            .toImmutableList()
    }

    data class DeleteWalletModel(
        val cacheState: WalletState.ContentState,
        val action: WalletsUpdateActionResolver.Action.DeleteWallet,
    )
}