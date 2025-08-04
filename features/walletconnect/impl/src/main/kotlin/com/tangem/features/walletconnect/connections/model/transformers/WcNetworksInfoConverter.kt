package com.tangem.features.walletconnect.connections.model.transformers

import com.tangem.core.ui.extensions.iconResId
import com.tangem.domain.models.network.Network
import com.tangem.features.walletconnect.connections.entity.WcNetworkInfoItem
import com.tangem.features.walletconnect.connections.entity.WcNetworksInfo
import com.tangem.utils.converter.Converter
import kotlinx.collections.immutable.toImmutableList

@Suppress("DestructuringDeclarationWithTooManyEntries")
internal object WcNetworksInfoConverter : Converter<WcNetworksInfoConverter.Input, WcNetworksInfo> {
    override fun convert(value: Input): WcNetworksInfo {
        val (missing, required, available, notAdded, additionallyEnabled) = value
        return when {
            missing.isNotEmpty() -> WcNetworksInfo.MissingRequiredNetworkInfo(missing.joinToString { it.name })
            required.isEmpty() && available.isEmpty() && notAdded.isNotEmpty() -> WcNetworksInfo.NoneNetworksAdded
            else -> {
                val combinedNetworks = required + additionallyEnabled
                WcNetworksInfo.ContainsAllRequiredNetworks(
                    items = combinedNetworks.map { network ->
                        WcNetworkInfoItem.Required(
                            id = network.rawId,
                            icon = network.iconResId,
                            name = network.name,
                            symbol = network.currencySymbol,
                        )
                    }.toImmutableList(),
                )
            }
        }
    }

    data class Input(
        val missingNetworks: Set<Network>,
        val requiredNetworks: Set<Network>,
        val availableNetworks: Set<Network>,
        val notAddedNetworks: Set<Network>,
        val additionallyEnabledNetworks: Set<Network>,
    )
}