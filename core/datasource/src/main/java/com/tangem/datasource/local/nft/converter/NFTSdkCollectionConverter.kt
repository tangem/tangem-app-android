package com.tangem.datasource.local.nft.converter

import com.tangem.domain.nft.models.NFTAsset
import com.tangem.domain.nft.models.NFTCollection
import com.tangem.domain.tokens.model.Network
import com.tangem.utils.converter.Converter
import com.tangem.blockchain.nft.models.NFTCollection as SdkNFTCollection

object NFTSdkCollectionConverter : Converter<Pair<Network, SdkNFTCollection>, NFTCollection> {
    override fun convert(value: Pair<Network, SdkNFTCollection>): NFTCollection {
        val (network, collection) = value
        val collectionId = NFTSdkCollectionIdentifierConverter.convert(collection.identifier)
        return NFTCollection(
            id = collectionId,
            network = network,
            name = collection.name,
            description = collection.description,
            logoUrl = collection.logoUrl,
            count = collection.count,
            assets = collection.assets
                .map { asset ->
                    NFTSdkAssetConverter.convert(network to asset)
                }
                .filter {
                    it.id !is NFTAsset.Identifier.Unknown
                },
        )
    }
}