package com.tangem.features.nft.receive.entity.transformer

import com.tangem.features.nft.receive.entity.NFTReceiveUM
import com.tangem.utils.transformer.Transformer

internal class ToggleSearchBarTransformer(private val isActive: Boolean) : Transformer<NFTReceiveUM> {

    override fun transform(prevState: NFTReceiveUM): NFTReceiveUM = prevState.copy(
        search = prevState.search.copy(
            isActive = isActive,
        ),
    )
}