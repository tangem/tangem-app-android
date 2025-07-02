package com.tangem.features.walletconnect.connections.model.transformers

import com.tangem.features.walletconnect.connections.entity.WcSelectNetworksUM
import com.tangem.utils.transformer.Transformer
import kotlinx.collections.immutable.toImmutableList

internal class WcSelectNetworksCheckedTransformer(
    private val networkId: String,
    private val isChecked: Boolean,
) : Transformer<WcSelectNetworksUM> {
    override fun transform(prevState: WcSelectNetworksUM): WcSelectNetworksUM {
        return prevState.copy(
            available = prevState.available
                .map { item -> if (item.id == networkId) item.copy(checked = isChecked) else item }
                .toImmutableList(),
        )
    }
}