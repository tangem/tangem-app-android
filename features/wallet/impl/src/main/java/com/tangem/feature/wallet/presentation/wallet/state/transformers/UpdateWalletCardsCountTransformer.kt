package com.tangem.feature.wallet.presentation.wallet.state.transformers

import com.tangem.domain.card.common.util.getCardsCount
import com.tangem.domain.models.wallet.UserWallet
import com.tangem.feature.wallet.presentation.wallet.domain.WalletAdditionalInfoFactory
import com.tangem.feature.wallet.presentation.wallet.domain.WalletImageResolver
import com.tangem.feature.wallet.presentation.wallet.state.model.TokenSyncProgressUM
import com.tangem.feature.wallet.presentation.wallet.state.model.WalletCardState
import com.tangem.feature.wallet.presentation.wallet.state.model.WalletState
import com.tangem.feature.wallet.presentation.wallet.state.model.WalletUM
import com.tangem.utils.logging.TangemLogger

internal class UpdateWalletCardsCountTransformer(
    private val userWallet: UserWallet,
    private val walletImageResolver: WalletImageResolver,
) : WalletStateTransformer(userWallet.walletId) {

    override fun transform(prevState: WalletState): WalletState {
        return when (prevState) {
            is WalletState.MultiCurrency.Content -> {
                prevState.copy(
                    walletCardState = prevState.walletCardState.toUpdatedState(prevState.tokenSyncProgressUM),
                )
            }
            is WalletState.SingleCurrency.Content -> {
                prevState.copy(walletCardState = prevState.walletCardState.toUpdatedState())
            }
            is WalletState.MultiCurrency.Locked,
            is WalletState.SingleCurrency.Locked,
            -> {
                TangemLogger.e("Impossible to update wallet cards count for locked wallet")
                prevState
            }
        }
    }

    override fun transform(walletUM: WalletUM): WalletUM {
        return walletUM // todo redesign main
    }

    private fun WalletCardState.toUpdatedState(
        syncProgress: TokenSyncProgressUM = TokenSyncProgressUM.Idle,
    ): WalletCardState {
        return when (this) {
            is WalletCardState.Content -> copy(
                additionalInfo = WalletAdditionalInfoFactory.resolve(wallet = userWallet, syncProgress = syncProgress),
                imageResId = walletImageResolver.resolve(userWallet = userWallet),
                cardCount = (userWallet as? UserWallet.Cold)?.getCardsCount(),
            )
            else -> this
        }
    }
}