package com.tangem.feature.wallet.presentation.wallet.state2.transformers

import com.tangem.domain.wallets.models.UserWallet
import com.tangem.domain.wallets.models.UserWalletId
import com.tangem.feature.wallet.presentation.wallet.state.components.WalletTopBarConfig
import com.tangem.feature.wallet.presentation.wallet.state2.WalletScreenState
import com.tangem.feature.wallet.presentation.wallet.state2.WalletState
import com.tangem.feature.wallet.presentation.wallet.viewmodels.intents.WalletClickIntentsV2
import kotlinx.collections.immutable.toImmutableList
import timber.log.Timber
import com.tangem.feature.wallet.presentation.wallet.viewmodels.WalletsUpdateActionResolverV2.Action.UnlockWallet as UnlockWalletAction

/**
[REDACTED_AUTHOR]
 */
internal class UnlockWalletTransformer(
    private val action: UnlockWalletAction,
    private val clickIntents: WalletClickIntentsV2,
) : WalletScreenStateTransformer {

    private val walletStateFactory by lazy { WalletStateFactory(clickIntents = clickIntents) }

    override fun transform(prevState: WalletScreenState): WalletScreenState {
        return prevState.copy(
            topBarConfig = prevState.topBarConfig.toUnlockedState(),
            wallets = prevState.wallets
                .map { state ->
                    val unlockedWallet = action.getUnlockedWallet(state.walletCardState.id)
                    if (unlockedWallet == null) state else createLoadingState(state, unlockedWallet)
                }
                .toImmutableList(),
        )
    }

    private fun WalletTopBarConfig.toUnlockedState(): WalletTopBarConfig {
        return copy(onDetailsClick = clickIntents::onDetailsClick)
    }

    private fun UnlockWalletAction.getUnlockedWallet(walletId: UserWalletId): UserWallet? {
        return unlockedWallets.firstOrNull { it.walletId == walletId }
    }

    private fun createLoadingState(prevState: WalletState, unlockedWallet: UserWallet): WalletState {
        return when (prevState) {
            is WalletState.MultiCurrency.Locked -> {
                walletStateFactory.createLoadingMultiCurrencyContent(userWallet = unlockedWallet)
            }
            is WalletState.SingleCurrency.Locked -> {
                walletStateFactory.createLoadingSingleCurrencyContent(userWallet = unlockedWallet)
            }
            is WalletState.MultiCurrency.Content,
            is WalletState.SingleCurrency.Content,
            -> {
                Timber.e("Impossible to unlock wallet with content state")
                prevState
            }
        }
    }
}