package com.tangem.datasource.local.nft.converter

import android.content.res.Resources
import com.tangem.datasource.R
import com.tangem.domain.models.StatusSource
import com.tangem.domain.models.network.Network
import com.tangem.domain.nft.models.NFTAsset
import com.tangem.domain.nft.models.NFTCollection
import com.tangem.utils.converter.Converter
import com.tangem.blockchain.nft.models.NFTCollection as SdkNFTCollection

class NFTSdkCollectionConverter(
    private val resources: Resources,
) : Converter<Pair<Network, SdkNFTCollection>, NFTCollection> {
    override fun convert(value: Pair<Network, SdkNFTCollection>): NFTCollection {
        val (network, collection) = value
        val collectionId = NFTSdkCollectionIdentifierConverter.convert(collection.identifier)
        return NFTCollection(
            id = collectionId,
            network = network,
            // We use localised strings from resources here to proceed sorting and searching correctly
            // Sorting is invoked on data layer, searching is simplified to filtering for now and invoked in Model
            name = collection.toName(collectionId),
            description = collection.description,
            logoUrl = collection.logoUrl,
            count = collection.count,
            assets = collection.assets
                .map { asset ->
                    NFTSdkAssetConverter.convert(network to asset)
                }
                .filter {
                    it.id !is NFTAsset.Identifier.Unknown
                }
                .let {
                    if (it.isEmpty()) {
                        NFTCollection.Assets.Empty
                    } else {
                        NFTCollection.Assets.Value(
                            items = it,
                            source = StatusSource.CACHE,
                        )
                    }
                },
        )
    }

    private fun SdkNFTCollection.toName(collectionId: NFTCollection.Identifier) = when (collectionId) {
        is NFTCollection.Identifier.EVM ->
            name.toCollectionName()
        is NFTCollection.Identifier.TON -> if (collectionId.contractAddress == null) {
            resources.getString(R.string.nft_no_collection)
        } else {
            name.toCollectionName()
        }
        is NFTCollection.Identifier.Solana -> if (collectionId.collectionAddress == null) {
            resources.getString(R.string.nft_no_collection)
        } else {
            name.toCollectionName()
        }
        NFTCollection.Identifier.Unknown -> null
    }

    private fun String?.toCollectionName() = if (this.isNullOrEmpty()) {
        resources.getString(R.string.nft_untitled_collection)
    } else {
        this
    }
}