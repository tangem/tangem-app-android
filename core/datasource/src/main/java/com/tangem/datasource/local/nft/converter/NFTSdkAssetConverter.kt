package com.tangem.datasource.local.nft.converter

import com.tangem.domain.models.StatusSource
import com.tangem.domain.nft.models.NFTAsset
import com.tangem.domain.nft.models.NFTSalePrice
import com.tangem.domain.tokens.model.Network
import com.tangem.utils.converter.Converter
import com.tangem.blockchain.nft.models.NFTAsset as SdkNFTAsset

object NFTSdkAssetConverter : Converter<Pair<Network, SdkNFTAsset>, NFTAsset> {
    override fun convert(value: Pair<Network, SdkNFTAsset>): NFTAsset {
        val (network, asset) = value
        val assetId = NFTSdkAssetIdentifierConverter.convert(asset.identifier)
        val collectionId = NFTSdkCollectionIdentifierConverter.convert(asset.collectionIdentifier)
        return NFTAsset(
            id = assetId,
            collectionId = collectionId,
            network = network,
            contractType = asset.contractType,
            owner = asset.owner,
            name = asset.name,
            description = asset.description,
            salePrice = asset.salePrice?.let {
                NFTSalePrice.Value(
                    assetId = assetId,
                    value = it.value,
                    symbol = it.symbol,
                )
            } ?: NFTSalePrice.Empty(assetId = assetId),
            rarity = asset.rarity?.let {
                NFTAsset.Rarity(
                    rank = it.rank,
                    label = it.label,
                )
            },
            media = asset.media?.let {
                NFTAsset.Media(
                    url = it.url,
                    mimetype = it.mimetype,
                )
            },
            traits = asset.traits.map {
                NFTAsset.Trait(
                    name = it.name,
                    value = it.value,
                )
            },
            source = StatusSource.CACHE,
        )
    }
}