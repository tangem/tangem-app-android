package com.tangem.features.nft.component

import com.tangem.core.decompose.factory.ComponentFactory
import com.tangem.core.ui.decompose.ComposableContentComponent
import com.tangem.domain.nft.models.NFTAsset

interface NFTAssetTraitsComponent : ComposableContentComponent {

    data class Params(
        val nftAsset: NFTAsset,
    )

    interface Factory : ComponentFactory<Params, NFTAssetTraitsComponent>
}