package com.tangem.domain.nft.models

import com.tangem.blockchain.common.Blockchain
import com.tangem.blockchain.nft.models.NFTAsset
import com.tangem.blockchain.nft.models.NFTCollection
import com.tangem.domain.models.StatusSource

sealed interface NFTCollections {
    val blockchain: Blockchain

    data class Error(
        override val blockchain: Blockchain,
    ) : NFTCollections

    data class Value(
        override val blockchain: Blockchain,
        val collections: List<Collection>,
    ) : NFTCollections

    sealed class Collection {

        abstract val collection: NFTCollection

        data class Value(
            override val collection: NFTCollection,
            val salePrices: Map<NFTAsset.Identifier, NFTSalePrice>,
            val source: StatusSource,
        ) : Collection()

        data class Error(
            override val collection: NFTCollection,
            val error: Throwable,
        ) : Collection()
    }
}
