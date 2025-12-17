package com.tangem.feature.wallet.presentation.wallet.state.transformers

import com.tangem.domain.models.wallet.UserWallet
import com.tangem.feature.wallet.child.wallet.model.intents.WalletClickIntents
import com.tangem.feature.wallet.presentation.wallet.domain.WalletImageResolver
import com.tangem.feature.wallet.presentation.wallet.state.model.WalletState
import com.tangem.feature.wallet.presentation.wallet.state.utils.WalletLoadingStateFactory

/**
 * Reinitialize wallet transformer
 *
 * @property userWallet   user wallet to reinitialize
 * @property clickIntents click intents
 * @property walletImageResolver wallet image resolver
 */
internal class ReinitializeWalletTransformer(
    private val userWallet: UserWallet,
    private val clickIntents: WalletClickIntents,
    private val walletImageResolver: WalletImageResolver,
) : WalletStateTransformer(userWalletId = userWallet.walletId) {

    private val walletLoadingStateFactory by lazy {
        WalletLoadingStateFactory(
            clickIntents = clickIntents,
            walletImageResolver = walletImageResolver,
        )
    }

    override fun transform(prevState: WalletState): WalletState {
        return walletLoadingStateFactory.create(
            userWallet = userWallet,
        )
    }
}