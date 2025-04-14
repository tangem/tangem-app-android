package com.tangem.domain.nft.models

import com.tangem.domain.models.StatusSource
import com.tangem.domain.tokens.model.Network
import kotlinx.serialization.Serializable

@Serializable
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

    @Serializable
    data class Media(
        val mimetype: String?,
        val url: String,
    )

    @Serializable
    data class Rarity(
        val rank: String,
        val label: String,
    )

    @Serializable
    data class Trait(
        val name: String,
        val value: String,
    )

    @Serializable
    sealed class Identifier {
        abstract val stringValue: String

        @Serializable
        data class EVM(
            val tokenId: String,
            val tokenAddress: String,
        ) : Identifier() {
            override val stringValue: String = "${tokenAddress}_$tokenId"
        }

        @Serializable
        data class TON(val tokenAddress: String) : Identifier() {
            override val stringValue: String = tokenAddress
        }

        @Serializable
        data object Unknown : Identifier() {
            override val stringValue: String = ""
        }
    }
}