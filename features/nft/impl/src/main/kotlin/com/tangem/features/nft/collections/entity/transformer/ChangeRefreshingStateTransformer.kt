package com.tangem.features.nft.collections.entity.transformer

import com.tangem.features.nft.collections.entity.NFTCollectionsStateUM
import com.tangem.utils.transformer.Transformer

internal class ChangeRefreshingStateTransformer(
    private val isRefreshing: Boolean,
) : Transformer<NFTCollectionsStateUM> {

    override fun transform(prevState: NFTCollectionsStateUM): NFTCollectionsStateUM = prevState.copy(
        pullToRefreshConfig = prevState.pullToRefreshConfig.copy(
            isRefreshing = isRefreshing,
        ),
    )
}