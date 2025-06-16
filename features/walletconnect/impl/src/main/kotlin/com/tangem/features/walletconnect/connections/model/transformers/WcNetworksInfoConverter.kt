package com.tangem.features.walletconnect.connections.model.transformers

import com.tangem.core.ui.extensions.iconResId
import com.tangem.domain.models.network.Network
import com.tangem.features.walletconnect.connections.entity.WcNetworkInfoItem
import com.tangem.features.walletconnect.connections.entity.WcNetworksInfo
import com.tangem.utils.converter.Converter
import kotlinx.collections.immutable.toImmutableList

internal object WcNetworksInfoConverter : Converter<WcNetworksInfoConverter.Input, WcNetworksInfo> {
    override fun convert(value: Input): WcNetworksInfo {
        return if (value.missingNetworks.isNotEmpty()) {
            WcNetworksInfo.MissingRequiredNetworkInfo(
                networks = value.missingNetworks.joinToString { it.name },
            )
        } else {
            WcNetworksInfo.ContainsAllRequiredNetworks(
                items = (value.requiredNetworks + value.additionallyEnabledNetworks)
                    .map {
                        WcNetworkInfoItem.Required(
                            id = it.rawId,
                            icon = it.iconResId,
                            name = it.name,
                            symbol = it.currencySymbol,
                        )
                    }
                    .toImmutableList(),
            )
        }
    }

    data class Input(
        val missingNetworks: Set<Network>,
        val requiredNetworks: Set<Network>,
        val additionallyEnabledNetworks: Set<Network>,
    )
}