package com.tangem.features.nft.component

import com.tangem.core.decompose.factory.ComponentFactory
import com.tangem.core.ui.decompose.ComposableContentComponent
import com.tangem.domain.wallets.models.UserWalletId

interface NFTComponent : ComposableContentComponent {

    data class Params(
        val userWalletId: UserWalletId,
        val walletName: String,
    )

    interface Factory : ComponentFactory<Params, NFTComponent>
}