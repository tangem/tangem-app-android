package com.tangem.domain.nft.models

import com.tangem.domain.tokens.model.Network

data class NFTCollection(
    val id: Identifier,
    val network: Network,
    val name: String?,
    val description: String?,
    val logoUrl: String?,
    val count: Int,
    val assets: List<NFTAsset> = emptyList(),
) {
    sealed class Identifier {
        data class EVM(val tokenAddress: String) : Identifier()
        data class TON(val contractAddress: String?) : Identifier()
        data object Unknown : Identifier()
    }
}