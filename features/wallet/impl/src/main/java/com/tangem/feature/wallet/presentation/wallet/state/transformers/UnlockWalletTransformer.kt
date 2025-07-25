package com.tangem.feature.wallet.presentation.wallet.state.transformers

import com.tangem.domain.models.wallet.UserWallet
import com.tangem.domain.models.wallet.UserWalletId
import com.tangem.feature.wallet.presentation.wallet.domain.WalletImageResolver
import com.tangem.feature.wallet.presentation.wallet.state.model.WalletScreenState
import com.tangem.feature.wallet.presentation.wallet.state.model.WalletState
import com.tangem.feature.wallet.presentation.wallet.state.utils.WalletLoadingStateFactory
import com.tangem.feature.wallet.child.wallet.model.intents.WalletClickIntents
import kotlinx.collections.immutable.toImmutableList
import timber.log.Timber

internal class UnlockWalletTransformer(
    private val unlockedWallets: List<UserWallet>,
    private val clickIntents: WalletClickIntents,
    private val walletImageResolver: WalletImageResolver,
) : WalletScreenStateTransformer {

    private val walletLoadingStateFactory by lazy {
        WalletLoadingStateFactory(
            clickIntents = clickIntents,
            walletImageResolver = walletImageResolver,
        )
    }

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
            -> walletLoadingStateFactory.create(
                userWallet = unlockedWallet,
            )
            is WalletState.MultiCurrency.Content,
            is WalletState.SingleCurrency.Content,
            is WalletState.Visa.Content,
            is WalletState.Visa.AccessTokenLocked,
            -> {
                Timber.e("Impossible to unlock wallet with not locked state")
                prevState
            }
        }
    }
}