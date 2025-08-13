package com.tangem.features.walletconnect.connections.model.transformers

import com.tangem.domain.models.network.Network
import com.tangem.features.walletconnect.connections.entity.WcSelectNetworksUM
import com.tangem.utils.transformer.Transformer
import kotlinx.collections.immutable.toImmutableList

internal class WcSelectNetworksCheckedTransformer(
    private val network: Network,
    private val isChecked: Boolean,
) : Transformer<WcSelectNetworksUM> {
    override fun transform(prevState: WcSelectNetworksUM): WcSelectNetworksUM {
        val available = prevState.available
            .map { item -> if (item.id == network.rawId) item.copy(checked = isChecked) else item }
        val doneButtonEnabled = available.any { it.checked } || prevState.required.isNotEmpty()
        return prevState.copy(
            available = available.toImmutableList(),
            doneButtonEnabled = doneButtonEnabled,
        )
    }
}