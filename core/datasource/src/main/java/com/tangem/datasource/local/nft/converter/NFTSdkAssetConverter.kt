package com.tangem.datasource.local.nft.converter

import com.tangem.domain.models.StatusSource
import com.tangem.domain.models.network.Network
import com.tangem.domain.nft.models.NFTAsset
import com.tangem.domain.nft.models.NFTSalePrice
import com.tangem.utils.converter.TwoWayConverter
import com.tangem.blockchain.nft.models.NFTAsset as SdkNFTAsset

object NFTSdkAssetConverter : TwoWayConverter<Pair<Network, SdkNFTAsset>, NFTAsset> {
    override fun convert(value: Pair<Network, SdkNFTAsset>): NFTAsset {
        val (network, asset) = value
        val assetId = NFTSdkAssetIdentifierConverter.convert(asset.identifier)
        val collectionId = NFTSdkCollectionIdentifierConverter.convert(asset.collectionIdentifier)
        val salePrice = asset.salePrice?.let { NFTSdkAssetSalePriceConverter(assetId).convert(it) }
            ?: NFTSalePrice.Empty(assetId)

        return NFTAsset(
            id = assetId,
            collectionId = collectionId,
            network = network,
            contractType = asset.contractType,
            owner = asset.owner,
            name = asset.name,
            description = asset.description,
            amount = asset.amount,
            decimals = asset.decimals,
            salePrice = salePrice,
            rarity = asset.rarity?.let { rarity ->
                NFTAsset.Rarity(
                    rank = rarity.rank,
                    label = rarity.label,
                )
            },
            media = asset.media?.let { media ->
                NFTAsset.Media(
                    animationUrl = media.animationUrl,
                    imageUrl = media.imageUrl,
                )
            },
            traits = asset.traits.map { trait ->
                NFTAsset.Trait(
                    name = trait.name,
                    value = trait.value,
                )
            },
            source = StatusSource.CACHE,
        )
    }

    override fun convertBack(value: NFTAsset): Pair<Network, SdkNFTAsset> {
        val assetId = NFTSdkAssetIdentifierConverter.convertBack(value.id)
        val collectionId = NFTSdkCollectionIdentifierConverter.convertBack(value.collectionId)
        val salePrice = (value.salePrice as? NFTSalePrice.Value)?.let {
            NFTSdkAssetSalePriceConverter(value.id).convertBack(it)
        }
        return value.network to SdkNFTAsset(
            identifier = assetId,
            collectionIdentifier = collectionId,
            blockchainId = value.network.rawId,
            contractType = value.contractType,
            owner = value.owner,
            name = value.name,
            description = value.description,
            amount = value.amount,
            decimals = value.decimals,
            salePrice = salePrice,
            rarity = value.rarity?.let { rarity ->
                SdkNFTAsset.Rarity(
                    rank = rarity.rank,
                    label = rarity.label,
                )
            },
            media = value.media?.let { media ->
                SdkNFTAsset.Media(
                    animationUrl = media.animationUrl,
                    imageUrl = media.imageUrl,
                )
            },
            traits = value.traits.map { trait ->
                SdkNFTAsset.Trait(
                    name = trait.name,
                    value = trait.value,
                )
            },
        )
    }
}