package com.tangem.feature.wallet.presentation.wallet.state.transformers

import com.tangem.domain.wallets.models.UserWallet
import com.tangem.domain.wallets.models.UserWalletId
import com.tangem.feature.wallet.child.wallet.model.intents.WalletClickIntents
import com.tangem.feature.wallet.presentation.wallet.domain.WalletImageResolver
import com.tangem.feature.wallet.presentation.wallet.state.model.WalletScreenState
import com.tangem.feature.wallet.presentation.wallet.state.utils.WalletLoadingStateFactory
import com.tangem.features.wallet.featuretoggles.WalletFeatureToggles
import kotlinx.collections.immutable.toImmutableList

/**
 * Reinitialize wallet transformer
 *
 * @property prevWalletId  reinitialized wallet id
 * @property newUserWallet new user wallet
 * @property clickIntents  click intents
 *
[REDACTED_AUTHOR]
 */
internal class ReinitializeWalletTransformer(
    private val prevWalletId: UserWalletId,
    private val newUserWallet: UserWallet,
    private val clickIntents: WalletClickIntents,
    private val walletImageResolver: WalletImageResolver,
    private val walletFeatureToggles: WalletFeatureToggles,
) : WalletScreenStateTransformer {

    private val walletLoadingStateFactory by lazy {
        WalletLoadingStateFactory(
            clickIntents = clickIntents,
            walletImageResolver = walletImageResolver,
            walletFeatureToggles = walletFeatureToggles,
        )
    }

    override fun transform(prevState: WalletScreenState): WalletScreenState {
        return prevState.copy(
            wallets = prevState.wallets
                .filterNot { it.walletCardState.id == prevWalletId }
                .plus(
                    element = walletLoadingStateFactory.create(
                        userWallet = newUserWallet,
                    ),
                )
                .toImmutableList(),
        )
    }
}