package com.tangem.features.walletconnect.connections.model.transformers

import com.tangem.core.ui.extensions.iconResId
import com.tangem.domain.walletconnect.model.WcSessionProposal
import com.tangem.features.walletconnect.connections.entity.WcNetworkInfoItem
import com.tangem.features.walletconnect.connections.entity.WcNetworksInfo
import com.tangem.utils.converter.Converter
import kotlinx.collections.immutable.toImmutableList

internal object WcNetworksInfoConverter : Converter<WcSessionProposal.ProposalNetwork, WcNetworksInfo> {
    override fun convert(value: WcSessionProposal.ProposalNetwork): WcNetworksInfo {
        return if (value.missingRequired.isNotEmpty()) {
            WcNetworksInfo.MissingRequiredNetworkInfo(
                networks = value.missingRequired
                    .joinToString { it.name },
            )
        } else {
            WcNetworksInfo.ContainsAllRequiredNetworks(
                items = (value.required + value.available)
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
}