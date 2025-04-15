package com.tangem.features.send.v2.api

import com.tangem.core.decompose.factory.ComponentFactory
import com.tangem.core.ui.decompose.ComposableContentComponent
import com.tangem.domain.nft.models.NFTAsset
import com.tangem.domain.wallets.models.UserWalletId

interface NFTSendComponent : ComposableContentComponent {

    data class Params(
        val userWalletId: UserWalletId,
        val nftAsset: NFTAsset,
        val nftCollectionName: String,
    )

    interface Factory : ComponentFactory<Params, NFTSendComponent>
}