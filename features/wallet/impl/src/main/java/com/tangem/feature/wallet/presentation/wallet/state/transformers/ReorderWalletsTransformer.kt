package com.tangem.feature.wallet.presentation.wallet.state.transformers

import com.tangem.domain.models.wallet.UserWallet
import com.tangem.feature.wallet.presentation.wallet.state.model.WalletScreenState
import kotlinx.collections.immutable.toImmutableList

/**
 * Transformer that reorders wallets according to the new order
 *
 * @property wallets wallets in the new order
 */
internal class ReorderWalletsTransformer(
    private val wallets: List<UserWallet>,
) : WalletScreenStateTransformer {

    override fun transform(prevState: WalletScreenState): WalletScreenState {
        val walletIdToIndex = wallets.withIndex().associate { it.value.walletId to it.index }

        val reorderedWallets = prevState.wallets
            .sortedBy { walletIdToIndex[it.walletCardState.id] ?: Int.MAX_VALUE }
            .toImmutableList()

        return if (reorderedWallets != prevState.wallets) {
            prevState.copy(wallets = reorderedWallets)
        } else {
            prevState
        }
    }
}