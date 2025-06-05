package com.tangem.domain.nft.models

import com.tangem.domain.models.StatusSource
import com.tangem.domain.models.network.Network

data class NFTCollections(
    val network: Network,
    val content: Content,
) {
    sealed class Content {
        data class Collections(
            val collections: List<NFTCollection>?,
            val source: StatusSource,
        ) : Content()

        data class Error(
            val error: Throwable,
        ) : Content()
    }

    companion object {
        fun empty(network: Network) = NFTCollections(
            network = network,
            content = Content.Collections(
                collections = null,
                source = StatusSource.ACTUAL,
            ),
        )
    }
}

fun List<NFTCollections>.allCollectionsFailed() = this.all {
    it.content is NFTCollections.Content.Error
}

fun List<NFTCollections>.anyCollectionFailed() = this.any {
    it.content is NFTCollections.Content.Error ||
        it.content is NFTCollections.Content.Collections &&
        it.content.source == StatusSource.ONLY_CACHE
}

fun List<NFTCollections>.allLoadedCollectionsEmpty() = this
    .map { it.content }
    .filterIsInstance<NFTCollections.Content.Collections>()
    .all { it.collections.isNullOrEmpty() }

fun List<NFTCollections>.allCollectionsLoaded() = this.all {
    val content = it.content
    content is NFTCollections.Content.Collections &&
        content.source != StatusSource.CACHE
}

fun List<NFTCollections>.allCollectionsEmpty() = this.all {
    val content = it.content
    content is NFTCollections.Content.Collections &&
        content.collections.isNullOrEmpty()
}