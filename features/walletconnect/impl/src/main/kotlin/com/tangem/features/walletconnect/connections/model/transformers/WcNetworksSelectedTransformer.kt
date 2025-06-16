package com.tangem.features.walletconnect.connections.model.transformers

import com.tangem.domain.models.network.Network
import com.tangem.features.walletconnect.connections.entity.WcAppInfoUM
import com.tangem.utils.transformer.Transformer

internal class WcNetworksSelectedTransformer(
    private val missingNetworks: Set<Network>,
    private val requiredNetworks: Set<Network>,
    private val additionallyEnabledNetworks: Set<Network>,
) : Transformer<WcAppInfoUM> {
    override fun transform(prevState: WcAppInfoUM): WcAppInfoUM {
        val contentState = prevState as? WcAppInfoUM.Content ?: return prevState
        return contentState.copy(
            connectButtonConfig = contentState.connectButtonConfig.copy(
                enabled = missingNetworks.isEmpty() &&
                    (requiredNetworks.isNotEmpty() || additionallyEnabledNetworks.isNotEmpty()),
            ),
            networksInfo = WcNetworksInfoConverter.convert(
                value = WcNetworksInfoConverter.Input(
                    missingNetworks = missingNetworks,
                    requiredNetworks = requiredNetworks,
                    additionallyEnabledNetworks = additionallyEnabledNetworks,
                ),
            ),
        )
    }
}