package com.tangem.features.nft.details.entity.transformer

import com.tangem.features.nft.details.entity.NFTAssetUM
import com.tangem.features.nft.details.entity.NFTDetailsUM
import com.tangem.utils.transformer.Transformer

internal object NFTPriceUpdatingTransformer : Transformer<NFTDetailsUM> {

    override fun transform(prevState: NFTDetailsUM): NFTDetailsUM {
        val topInfo = prevState.nftAsset.topInfo as? NFTAssetUM.TopInfo.Content ?: return prevState
        val salePrice = topInfo.salePrice as? NFTAssetUM.SalePrice.Content
        return prevState.copy(
            nftAsset = prevState.nftAsset.copy(
                topInfo = topInfo.copy(
                    salePrice = salePrice?.copy(
                        isFlickering = true,
                    ) ?: topInfo.salePrice,
                ),
            ),
        )
    }
}