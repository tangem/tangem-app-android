package com.tangem.features.walletconnect.connections.model.transformers

import com.tangem.features.walletconnect.connections.entity.WcSelectNetworksUM
import com.tangem.utils.transformer.Transformer
import kotlinx.collections.immutable.toImmutableList

internal object WcSelectNetworksClearCheckedTransformer : Transformer<WcSelectNetworksUM> {
    override fun transform(prevState: WcSelectNetworksUM): WcSelectNetworksUM {
        return prevState.copy(
            available = prevState.available.map { it.copy(checked = false) }.toImmutableList(),
        )
    }
}