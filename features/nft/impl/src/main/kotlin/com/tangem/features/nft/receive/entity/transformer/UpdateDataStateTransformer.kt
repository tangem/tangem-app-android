package com.tangem.features.nft.receive.entity.transformer

import com.tangem.core.ui.components.currency.icon.CurrencyIconState
import com.tangem.core.ui.components.rows.model.ChainRowUM
import com.tangem.core.ui.extensions.getActiveIconRes
import com.tangem.domain.models.network.Network
import com.tangem.domain.nft.models.NFTNetworks
import com.tangem.features.nft.receive.entity.NFTNetworkUM
import com.tangem.features.nft.receive.entity.NFTReceiveUM
import com.tangem.utils.transformer.Transformer
import kotlinx.collections.immutable.toPersistentList

internal class UpdateDataStateTransformer(
    private val networks: NFTNetworks,
    private val onNetworkClick: (Network, Boolean) -> Unit,
) : Transformer<NFTReceiveUM> {

    override fun transform(prevState: NFTReceiveUM): NFTReceiveUM = prevState.copy(
        networks = when {
            networks.availableNetworks.isEmpty() && networks.unavailableNetworks.isEmpty()
            -> NFTReceiveUM.Networks.Empty
            else -> NFTReceiveUM.Networks.Content(
                availableItems = networks.availableNetworks
                    .map { it.transform(true) }
                    .toPersistentList(),
                unavailableItems = networks.unavailableNetworks
                    .map { it.transform(false) }
                    .toPersistentList(),
            )
        },
    )

    private fun Network.transform(enabled: Boolean): NFTNetworkUM {
        val custom = derivationPath is Network.DerivationPath.Custom
        return NFTNetworkUM(
            id = rawId + derivationPath.value,
            chainRowUM = ChainRowUM(
                name = name,
                type = "",
                icon = CurrencyIconState.CoinIcon(
                    url = null,
                    fallbackResId = getActiveIconRes(rawId),
                    isGrayscale = !enabled,
                    showCustomBadge = custom,
                ),
                showCustom = custom,
                enabled = enabled,
            ),
            onItemClick = {
                onNetworkClick(this, enabled)
            },
        )
    }
}