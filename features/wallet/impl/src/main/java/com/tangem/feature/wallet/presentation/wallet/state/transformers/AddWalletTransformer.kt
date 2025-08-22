package com.tangem.feature.wallet.presentation.wallet.state.transformers

import com.tangem.domain.models.wallet.UserWallet
import com.tangem.feature.wallet.child.wallet.model.intents.WalletClickIntents
import com.tangem.feature.wallet.presentation.wallet.domain.WalletImageResolver
import com.tangem.feature.wallet.presentation.wallet.state.model.WalletScreenState
import com.tangem.feature.wallet.presentation.wallet.state.utils.WalletLoadingStateFactory
import kotlinx.collections.immutable.toImmutableList

internal class AddWalletTransformer(
    private val userWallet: UserWallet,
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
            wallets = (prevState.wallets + walletLoadingStateFactory.create(userWallet)).toImmutableList(),
        )
    }
}