package com.tangem.features.nft.details.block

import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import com.tangem.core.decompose.context.AppComponentContext
import com.tangem.core.ui.extensions.getActiveIconRes
import com.tangem.core.ui.extensions.stringReference
import com.tangem.features.nft.component.NFTDetailsBlockComponent
import com.tangem.features.nft.details.block.ui.NFTDetailsBlock
import dagger.assisted.Assisted
import dagger.assisted.AssistedFactory
import dagger.assisted.AssistedInject

class DefaultNFTDetailsBlockComponent @AssistedInject constructor(
    @Assisted context: AppComponentContext,
    @Assisted private val params: NFTDetailsBlockComponent.Params,
) : NFTDetailsBlockComponent, AppComponentContext by context {

    @Composable
    override fun Content(modifier: Modifier) {
        NFTDetailsBlock(
            assetName = stringReference(params.nftAsset.name.orEmpty()),
            collectionName = stringReference(params.nftCollectionName),
            assetImage = params.nftAsset.media?.url,
            networkIconRes = getActiveIconRes(params.nftAsset.network.rawId),
        )
    }

    @AssistedFactory
    interface Factory : NFTDetailsBlockComponent.Factory {
        override fun create(
            context: AppComponentContext,
            params: NFTDetailsBlockComponent.Params,
        ): DefaultNFTDetailsBlockComponent
    }
}