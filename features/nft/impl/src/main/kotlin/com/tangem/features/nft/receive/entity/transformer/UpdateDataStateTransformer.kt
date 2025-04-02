package com.tangem.features.nft.receive.entity.transformer

import com.tangem.core.ui.extensions.getActiveIconRes
import com.tangem.domain.tokens.model.Network
import com.tangem.features.nft.receive.entity.NFTNetworkUM
import com.tangem.features.nft.receive.entity.NFTReceiveUM
import com.tangem.utils.transformer.Transformer
import kotlinx.collections.immutable.toPersistentList

internal class UpdateDataStateTransformer(
    private val networks: List<Network>,
    private val onNetworkClick: (Network) -> Unit,
) : Transformer<NFTReceiveUM> {

    override fun transform(prevState: NFTReceiveUM): NFTReceiveUM = prevState.copy(
        networks = when {
            networks.isEmpty() -> NFTReceiveUM.Networks.Empty
            else -> NFTReceiveUM.Networks.Content(
                items = networks
                    .map { it.transform() }
                    .toPersistentList(),
            )
        },
    )

    private fun Network.transform(): NFTNetworkUM = NFTNetworkUM(
        id = id.value,
        iconRes = getActiveIconRes(id.value),
        name = name,
        onItemClick = {
            onNetworkClick(this)
        },
    )
}