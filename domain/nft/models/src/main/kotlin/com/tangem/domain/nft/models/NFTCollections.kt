package com.tangem.domain.nft.models

import com.tangem.domain.models.StatusSource
import com.tangem.domain.tokens.model.Network

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
}