package com.tangem.features.nft.component

import com.tangem.core.decompose.factory.ComponentFactory
import com.tangem.core.ui.decompose.ComposableContentComponent
import com.tangem.core.ui.extensions.TextReference
import com.tangem.domain.models.wallet.UserWalletId
import com.tangem.domain.nft.models.NFTAsset

interface NFTDetailsBlockComponent : ComposableContentComponent {

    data class Params(
        val userWalletId: UserWalletId,
        val nftAsset: NFTAsset,
        val nftCollectionName: String,
        val title: TextReference,
        val isSuccessScreen: Boolean,
    )

    interface Factory : ComponentFactory<Params, NFTDetailsBlockComponent>
}