package com.tangem.domain.nft.models

import com.tangem.domain.models.StatusSource
import com.tangem.domain.tokens.model.Network
import kotlinx.serialization.Serializable

@Serializable
data class NFTCollection(
    val id: Identifier,
    val network: Network,
    val name: String?,
    val description: String?,
    val logoUrl: String?,
    val count: Int,
    val assets: Assets,
) {
    @Serializable
    sealed class Assets {
        @Serializable
        data object Empty : Assets()

        @Serializable
        data object Loading : Assets()

        @Serializable
        data object Failed : Assets()

        @Serializable
        data class Value(
            val items: List<NFTAsset>,
            val source: StatusSource,
        ) : Assets()
    }

    @Serializable
    sealed class Identifier {
        @Serializable
        data class EVM(val tokenAddress: String) : Identifier()

        @Serializable
        data class TON(val contractAddress: String?) : Identifier()

        @Serializable
        data class Solana(val collection: String?) : Identifier()

        @Serializable
        data object Unknown : Identifier()
    }
}