package com.tangem.features.walletconnect.connections.model.transformers

import com.tangem.core.ui.extensions.getGreyedOutIconRes
import com.tangem.core.ui.extensions.iconResId
import com.tangem.domain.walletconnect.model.WcSessionProposal
import com.tangem.features.walletconnect.connections.entity.WcNetworkInfoItem
import com.tangem.features.walletconnect.connections.entity.WcSelectNetworksUM
import com.tangem.utils.transformer.Transformer
import kotlinx.collections.immutable.toImmutableList

internal class WcSelectNetworksTransformer(
    private val proposalNetwork: WcSessionProposal.ProposalNetwork,
    private val onCheckedChange: (Boolean, String) -> Unit,
) : Transformer<WcSelectNetworksUM> {
    override fun transform(prevState: WcSelectNetworksUM): WcSelectNetworksUM {
        return prevState.copy(
            missing = proposalNetwork.missingRequired.map { network ->
                WcNetworkInfoItem.Required(
                    id = network.id.value,
                    icon = network.iconResId,
                    name = network.name,
                    symbol = network.currencySymbol,
                )
            }.toImmutableList(),
            required = proposalNetwork.required.map { network ->
                WcNetworkInfoItem.Checked(
                    id = network.id.value,
                    icon = network.iconResId,
                    name = network.name,
                    symbol = network.currencySymbol,
                )
            }.toImmutableList(),
            available = proposalNetwork.available.map { network ->
                WcNetworkInfoItem.Checkable(
                    id = network.id.value,
                    icon = network.iconResId,
                    name = network.name,
                    symbol = network.currencySymbol,
                    checked = false,
                    onCheckedChange = { onCheckedChange(it, network.id.value) },
                )
            }.toImmutableList(),
            notAdded = proposalNetwork.notAdded.map { network ->
                WcNetworkInfoItem.ReadOnly(
                    id = network.id.value,
                    icon = getGreyedOutIconRes(network.id.value),
                    name = network.name,
                    symbol = network.currencySymbol,
                )
            }.toImmutableList(),
        )
    }
}