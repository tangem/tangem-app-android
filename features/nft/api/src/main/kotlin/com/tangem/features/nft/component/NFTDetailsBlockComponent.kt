package com.tangem.features.nft.component

import com.tangem.core.decompose.factory.ComponentFactory
import com.tangem.core.ui.decompose.ComposableContentComponent
import com.tangem.domain.nft.models.NFTAsset
import com.tangem.domain.wallets.models.UserWalletId

interface NFTDetailsBlockComponent : ComposableContentComponent {

    data class Params(
        val userWalletId: UserWalletId,
        val nftAsset: NFTAsset,
        val nftCollectionName: String,
    )

    interface Factory : ComponentFactory<Params, NFTDetailsBlockComponent>
}