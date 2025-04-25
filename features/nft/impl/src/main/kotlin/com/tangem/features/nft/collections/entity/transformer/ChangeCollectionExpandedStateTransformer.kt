package com.tangem.features.nft.collections.entity.transformer

import com.tangem.domain.nft.models.NFTCollection
import com.tangem.features.nft.collections.entity.NFTCollectionsStateUM
import com.tangem.features.nft.collections.entity.NFTCollectionsUM
import com.tangem.utils.transformer.Transformer
import kotlinx.collections.immutable.toPersistentList

internal class ChangeCollectionExpandedStateTransformer(
    private val collection: NFTCollection,
    private val collectionIdProvider: NFTCollection.() -> String,
    private val onFirstExpanded: () -> Unit,
) : Transformer<NFTCollectionsStateUM> {

    override fun transform(prevState: NFTCollectionsStateUM): NFTCollectionsStateUM = prevState.copy(
        content = when (prevState.content) {
            is NFTCollectionsUM.Empty,
            is NFTCollectionsUM.Loading,
            is NFTCollectionsUM.Failed,
            -> prevState.content
            is NFTCollectionsUM.Content -> prevState.content.copy(
                collections = prevState.content.collections.map {
                    val collectionId = collection.collectionIdProvider()
                    if (it.id == collectionId) {
                        if (!it.isExpanded) {
                            onFirstExpanded()
                        }
                        it.copy(isExpanded = !it.isExpanded)
                    } else {
                        it
                    }
                }.toPersistentList(),
            )
        },
    )
}