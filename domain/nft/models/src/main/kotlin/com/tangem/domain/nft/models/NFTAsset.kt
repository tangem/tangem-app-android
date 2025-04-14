package com.tangem.domain.nft.models

import com.tangem.domain.models.StatusSource
import com.tangem.domain.tokens.model.Network

sealed class NFTAsset {
    abstract val id: Identifier

    data class Value(
        override val id: Identifier,
        val collectionId: NFTCollection.Identifier,
        val network: Network,
        val contractType: String,
        val owner: String?,
        val name: String?,
        val description: String?,
        val salePrice: NFTSalePrice,
        val rarity: Rarity?,
        val media: Media?,
        val traits: List<Trait>,
        val source: StatusSource,
    ) : NFTAsset() {

        data class Media(
            val mimetype: String,
            val url: String,
        )

        data class Rarity(
            val rank: String,
            val label: String,
        )

        data class Trait(
            val name: String,
            val value: String,
        )
    }

    data class Error(
        override val id: Identifier,
    ) : NFTAsset()

    sealed class Identifier {
        data class EVM(
            val tokenId: String,
            val tokenAddress: String,
        ) : Identifier()

        data class TON(val tokenAddress: String) : Identifier()

        data object Unknown : Identifier()
    }
}