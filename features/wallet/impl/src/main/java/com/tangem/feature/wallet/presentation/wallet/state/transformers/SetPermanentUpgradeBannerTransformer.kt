package com.tangem.feature.wallet.presentation.wallet.state.transformers

import com.tangem.domain.models.wallet.UserWalletId
import com.tangem.feature.wallet.presentation.wallet.state.model.PermanentUpgradeBannerUM
import com.tangem.feature.wallet.presentation.wallet.state.model.WalletState
import com.tangem.feature.wallet.presentation.wallet.state.model.WalletUM

internal class SetPermanentUpgradeBannerTransformer(
    userWalletId: UserWalletId,
    val permanentUpgradeBannerUM: PermanentUpgradeBannerUM?,
) : WalletStateTransformer(userWalletId) {

    override fun transform(prevState: WalletState): WalletState = prevState

    override fun transform(walletUM: WalletUM): WalletUM {
        return when (walletUM) {
            is WalletUM.Content -> walletUM.copy(permanentUpgradeBannerUM = permanentUpgradeBannerUM)
            is WalletUM.Locked -> walletUM
        }
    }
}