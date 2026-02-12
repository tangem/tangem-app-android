package com.tangem.features.nft.mainEntry

import com.tangem.domain.nft.models.NFTCollection
import com.tangem.domain.nft.models.NFTCollections
import com.tangem.domain.nft.models.allLoadedCollectionsEmpty
import com.tangem.features.nft.entity.NFTBlockUM
import com.tangem.utils.transformer.Transformer
import kotlinx.collections.immutable.toPersistentList

internal class SetNFTCollectionsTransformer(
    private val nftCollections: List<NFTCollections>,
    private val onItemClick: () -> Unit,
) : Transformer<NFTBlockUM> {

    override fun transform(prevState: NFTBlockUM): NFTBlockUM {
        return when {
            nftCollections.allLoadedCollectionsEmpty() ->
                NFTBlockUM.Empty(onItemClick)
            else -> createContentNFTItemUM(onItemClick)
        }
    }

    private fun createContentNFTItemUM(onItemClick: () -> Unit): NFTBlockUM.Content {
        val collectionsContent = nftCollections
            .map { it.content }
            .filterIsInstance<NFTCollections.Content.Collections>()

        val collections = collectionsContent
            .mapNotNull { it.collections }
            .flatten()

        return NFTBlockUM.Content(
            previews = if (collections.size > NFT_COLLECTIONS_MAX_PREVIEWS_COUNT) {
                collections
                    .take(NFT_COLLECTIONS_MAX_PREVIEWS_COUNT - 1)
                    .map { NFTBlockUM.Content.CollectionPreview.Image(it.logoUrl.orEmpty()) }
                    .plus(NFTBlockUM.Content.CollectionPreview.More)
                    .toPersistentList()
            } else {
                collections
                    .take(NFT_COLLECTIONS_MAX_PREVIEWS_COUNT)
                    .map { NFTBlockUM.Content.CollectionPreview.Image(it.logoUrl.orEmpty()) }
                    .toPersistentList()
            },
            collectionsCount = collections.size,
            allAssetsCount = collections.sumOf { it.count },
            noCollectionAssetsCount = collections.sumOf { collection ->
                when (val collectionId = collection.id) {
                    is NFTCollection.Identifier.Solana ->
                        collection
                            .count
                            .takeIf { collectionId.collectionAddress == null }
                            ?: 0
                    is NFTCollection.Identifier.TON ->
                        collection
                            .count
                            .takeIf { collectionId.contractAddress == null }
                            ?: 0
                    is NFTCollection.Identifier.EVM,
                    is NFTCollection.Identifier.Unknown,
                    -> 0
                }
            },
            isFlickering = false,
            onItemClick = onItemClick,
        )
    }

    companion object {
        private const val NFT_COLLECTIONS_MAX_PREVIEWS_COUNT = 4
    }
}