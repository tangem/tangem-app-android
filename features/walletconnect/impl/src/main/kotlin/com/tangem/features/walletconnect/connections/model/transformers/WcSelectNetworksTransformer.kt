package com.tangem.features.walletconnect.connections.model.transformers

import com.tangem.core.ui.extensions.getGreyedOutIconRes
import com.tangem.core.ui.extensions.iconResId
import com.tangem.domain.models.network.Network
import com.tangem.features.walletconnect.connections.entity.WcNetworkInfoItem
import com.tangem.features.walletconnect.connections.entity.WcSelectNetworksUM
import com.tangem.utils.transformer.Transformer
import kotlinx.collections.immutable.toImmutableList

internal class WcSelectNetworksTransformer(
    private val missingRequiredNetworks: Set<Network>,
    private val requiredNetworks: Set<Network>,
    private val availableNetworks: Set<Network>,
    private val notAddedNetworks: Set<Network>,
    private val enabledNetworks: Set<Network.RawID>,
    private val onCheckedChange: (Boolean, String) -> Unit,
) : Transformer<WcSelectNetworksUM> {
    override fun transform(prevState: WcSelectNetworksUM): WcSelectNetworksUM {
        return prevState.copy(
            missing = missingRequiredNetworks.map { network ->
                WcNetworkInfoItem.Required(
                    id = network.rawId,
                    icon = network.iconResId,
                    name = network.name,
                    symbol = network.currencySymbol,
                )
            }.toImmutableList(),
            required = requiredNetworks.map { network ->
                WcNetworkInfoItem.Checked(
                    id = network.rawId,
                    icon = network.iconResId,
                    name = network.name,
                    symbol = network.currencySymbol,
                )
            }.toImmutableList(),
            available = availableNetworks.map { network ->
                WcNetworkInfoItem.Checkable(
                    id = network.rawId,
                    icon = network.iconResId,
                    name = network.name,
                    symbol = network.currencySymbol,
                    checked = network.id.rawId in enabledNetworks,
                    onCheckedChange = { onCheckedChange(it, network.rawId) },
                )
            }.toImmutableList(),
            notAdded = notAddedNetworks.map { network ->
                WcNetworkInfoItem.ReadOnly(
                    id = network.rawId,
                    icon = getGreyedOutIconRes(network.rawId),
                    name = network.name,
                    symbol = network.currencySymbol,
                )
            }.toImmutableList(),
        )
    }
}