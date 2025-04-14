package com.tangem.features.nft.receive.entity.transformer

import com.tangem.features.nft.receive.entity.NFTReceiveUM
import com.tangem.utils.transformer.Transformer

internal class UpdateSearchQueryTransformer(private val newQuery: String) : Transformer<NFTReceiveUM> {

    override fun transform(prevState: NFTReceiveUM): NFTReceiveUM = prevState.copy(
        search = prevState.search.copy(
            query = newQuery,
        ),
    )
}