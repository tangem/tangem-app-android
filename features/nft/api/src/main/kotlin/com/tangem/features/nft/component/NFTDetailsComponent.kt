package com.tangem.features.nft.component

import com.tangem.core.decompose.factory.ComponentFactory
import com.tangem.core.ui.decompose.ComposableContentComponent
import com.tangem.domain.nft.models.NFTAsset
import com.tangem.domain.wallets.models.UserWalletId

interface NFTDetailsComponent : ComposableContentComponent {

    data class Params(
        val userWalletId: UserWalletId,
        val nftAsset: NFTAsset,
    )

    interface Factory : ComponentFactory<Params, NFTDetailsComponent>
}