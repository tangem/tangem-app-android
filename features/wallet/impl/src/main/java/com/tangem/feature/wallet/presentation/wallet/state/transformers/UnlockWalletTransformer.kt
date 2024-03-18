package com.tangem.feature.wallet.presentation.wallet.state.transformers

import com.tangem.domain.wallets.models.UserWallet
import com.tangem.domain.wallets.models.UserWalletId
import com.tangem.feature.wallet.presentation.wallet.state.model.WalletScreenState
import com.tangem.feature.wallet.presentation.wallet.state.model.WalletState
import com.tangem.feature.wallet.presentation.wallet.state.utils.WalletLoadingStateFactory
import com.tangem.feature.wallet.presentation.wallet.viewmodels.intents.WalletClickIntents
import kotlinx.collections.immutable.toImmutableList
import timber.log.Timber

internal class UnlockWalletTransformer(
    private val unlockedWallets: List<UserWallet>,
    private val clickIntents: WalletClickIntents,
) : WalletScreenStateTransformer {

    private val walletLoadingStateFactory by lazy { WalletLoadingStateFactory(clickIntents = clickIntents) }

    override fun transform(prevState: WalletScreenState): WalletScreenState {
        return prevState.copy(
            wallets = prevState.wallets
                .map { state ->
                    val unlockedWallet = getUnlockedWallet(state.walletCardState.id)
                    if (unlockedWallet == null) state else createLoadingState(state, unlockedWallet)
                }
                .toImmutableList(),
        )
    }

    private fun getUnlockedWallet(walletId: UserWalletId): UserWallet? {
        return unlockedWallets.firstOrNull { it.walletId == walletId }
    }

    private fun createLoadingState(prevState: WalletState, unlockedWallet: UserWallet): WalletState {
        return when (prevState) {
            is WalletState.MultiCurrency.Locked,
            is WalletState.SingleCurrency.Locked,
            is WalletState.Visa.Locked,
            -> walletLoadingStateFactory.create(userWallet = unlockedWallet)
            is WalletState.MultiCurrency.Content,
            is WalletState.SingleCurrency.Content,
            is WalletState.Visa.Content,
            -> {
                Timber.e("Impossible to unlock wallet with content state")
                prevState
            }
        }
    }
}
