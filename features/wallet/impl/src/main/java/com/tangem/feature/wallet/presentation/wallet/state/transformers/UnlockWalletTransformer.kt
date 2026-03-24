package com.tangem.feature.wallet.presentation.wallet.state.transformers

import com.tangem.domain.models.wallet.UserWallet
import com.tangem.domain.models.wallet.UserWalletId
import com.tangem.domain.wallets.usecase.GetWalletIconUseCase
import com.tangem.feature.wallet.child.wallet.model.intents.WalletClickIntents
import com.tangem.feature.wallet.presentation.wallet.domain.WalletImageResolver
import com.tangem.feature.wallet.presentation.wallet.state.model.WalletScreenState
import com.tangem.feature.wallet.presentation.wallet.state.model.WalletState
import com.tangem.feature.wallet.presentation.wallet.state.model.WalletUM
import com.tangem.feature.wallet.presentation.wallet.state.utils.WalletLoadingStateFactory
import kotlinx.collections.immutable.toImmutableList
import kotlinx.collections.immutable.toPersistentList
import com.tangem.utils.logging.TangemLogger

internal class UnlockWalletTransformer(
    private val unlockedWallets: List<UserWallet>,
    private val clickIntents: WalletClickIntents,
    private val walletImageResolver: WalletImageResolver,
    private val getWalletIconUseCase: GetWalletIconUseCase,
    private val isTangemPayRefactorEnabled: Boolean,
) : WalletScreenStateTransformer {

    private val walletLoadingStateFactory by lazy {
        WalletLoadingStateFactory(
            clickIntents = clickIntents,
            walletImageResolver = walletImageResolver,
            getWalletIconUseCase = getWalletIconUseCase,
            isTangemPayRefactorEnabled = isTangemPayRefactorEnabled,
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
            wallets2 = prevState.wallets2
                .map { walletUM ->
                    val unlockedWallet = getUnlockedWallet(walletUM.walletsBalanceUM.id)
                    if (unlockedWallet == null) {
                        walletUM
                    } else {
                        createLoadingState2(
                            walletUM = walletUM,
                            unlockedWallet = unlockedWallet,
                        )
                    }
                }.toPersistentList(),
        )
    }

    private fun getUnlockedWallet(walletId: UserWalletId): UserWallet? {
        return unlockedWallets.firstOrNull { it.walletId == walletId }
    }

    private fun createLoadingState(prevState: WalletState, unlockedWallet: UserWallet): WalletState {
        return when (prevState) {
            is WalletState.MultiCurrency.Locked,
            is WalletState.SingleCurrency.Locked,
            -> walletLoadingStateFactory.create(
                userWallet = unlockedWallet,
            )
            is WalletState.MultiCurrency.Content,
            is WalletState.SingleCurrency.Content,
            -> {
                TangemLogger.e("Impossible to unlock wallet with not locked state")
                prevState
            }
        }
    }

    private fun createLoadingState2(walletUM: WalletUM, unlockedWallet: UserWallet): WalletUM {
        return when (walletUM) {
            is WalletUM.Locked -> walletLoadingStateFactory.create2(
                userWallet = unlockedWallet,
            )
            is WalletUM.Content -> {
                TangemLogger.e("Impossible to unlock wallet with not locked state")
                walletUM
            }
        }
    }
}