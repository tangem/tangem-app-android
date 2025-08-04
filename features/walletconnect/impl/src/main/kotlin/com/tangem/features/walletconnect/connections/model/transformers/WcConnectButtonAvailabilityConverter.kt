package com.tangem.features.walletconnect.connections.model.transformers

import com.tangem.domain.models.network.Network
import com.tangem.utils.converter.Converter

@Suppress("DestructuringDeclarationWithTooManyEntries")
internal object WcConnectButtonAvailabilityConverter : Converter<WcConnectButtonAvailabilityConverter.Input, Boolean> {

    override fun convert(value: Input): Boolean {
        val (missingNetworks, requiredNetworks, availableNetworks, selectedNetworks) = value

        return missingNetworks.isEmpty() &&
            (requiredNetworks.isNotEmpty() || availableNetworks.isNotEmpty() || selectedNetworks.isNotEmpty())
    }

    data class Input(
        val missingNetworks: Set<Network>,
        val requiredNetworks: Set<Network>,
        val availableNetworks: Set<Network>,
        val selectedNetworks: Set<Network>,
    )
}