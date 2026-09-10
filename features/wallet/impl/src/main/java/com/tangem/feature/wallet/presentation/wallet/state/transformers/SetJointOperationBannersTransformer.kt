package com.tangem.feature.wallet.presentation.wallet.state.transformers

import com.tangem.domain.models.wallet.UserWalletId
import com.tangem.feature.wallet.presentation.wallet.state.model.JointOperationBannerUM
import com.tangem.feature.wallet.presentation.wallet.state.model.WalletState
import com.tangem.feature.wallet.presentation.wallet.state.model.WalletUM
import kotlinx.collections.immutable.ImmutableList

internal class SetJointOperationBannersTransformer(
    userWalletId: UserWalletId,
    private val banners: ImmutableList<JointOperationBannerUM>,
) : WalletStateTransformer(userWalletId) {

    override fun transform(prevState: WalletState): WalletState = prevState

    override fun transform(walletUM: WalletUM): WalletUM {
        return when (walletUM) {
            is WalletUM.Content -> walletUM.copy(jointOperationBanners = banners)
            is WalletUM.Locked -> walletUM
        }
    }
}