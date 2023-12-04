package com.tangem.feature.wallet.presentation.wallet.state2.transformers

import com.tangem.domain.wallets.models.UserWallet
import com.tangem.feature.wallet.presentation.wallet.state2.WalletScreenState
import com.tangem.feature.wallet.presentation.wallet.state2.utils.WalletLoadingStateFactory
import com.tangem.feature.wallet.presentation.wallet.viewmodels.intents.WalletClickIntentsV2
import kotlinx.collections.immutable.toImmutableList

internal class AddWalletTransformer(
    private val userWallet: UserWallet,
    private val clickIntents: WalletClickIntentsV2,
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
