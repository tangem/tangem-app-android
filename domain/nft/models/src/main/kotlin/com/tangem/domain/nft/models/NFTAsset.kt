package com.tangem.domain.nft.models

import com.tangem.domain.models.StatusSource
import com.tangem.domain.tokens.model.Network

data class NFTAsset(
    val id: Identifier,
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
) {

    data class Media(
        val mimetype: String?,
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

    sealed class Identifier {
        data class EVM(
            val tokenId: String,
            val tokenAddress: String,
        ) : Identifier()

        data class TON(val tokenAddress: String) : Identifier()

        data object Unknown : Identifier()
    }
}