package com.tangem.feature.wallet.presentation.wallet.state2.transformers

import com.tangem.domain.wallets.models.UserWallet
import com.tangem.feature.wallet.presentation.wallet.state2.model.WalletScreenState
import com.tangem.feature.wallet.presentation.wallet.state2.utils.WalletLoadingStateFactory
import com.tangem.feature.wallet.presentation.wallet.viewmodels.intents.WalletClickIntents
import kotlinx.collections.immutable.toImmutableList

internal class AddWalletTransformer(
    private val userWallet: UserWallet,
    private val clickIntents: WalletClickIntents,
) : WalletScreenStateTransformer {

    private val walletLoadingStateFactory by lazy {
        WalletLoadingStateFactory(clickIntents = clickIntents)
    }

    override fun transform(prevState: WalletScreenState): WalletScreenState {
        return prevState.copy(
            wallets = (prevState.wallets + walletLoadingStateFactory.create(userWallet)).toImmutableList(),
        )
    }
}
