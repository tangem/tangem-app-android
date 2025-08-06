package com.tangem.features.walletconnect.connections.model.transformers

import com.tangem.domain.models.network.Network
import com.tangem.utils.converter.Converter

internal object WcConnectButtonAvailabilityConverter : Converter<WcConnectButtonAvailabilityConverter.Input, Boolean> {

    override fun convert(value: Input): Boolean {
        val missing = value.missingNetworks
        val required = value.requiredNetworks
        val available = value.availableNetworks
        val selected = value.selectedNetworks

        return missing.isEmpty() && (required.isNotEmpty() || available.isNotEmpty() || selected.isNotEmpty())
    }

    data class Input(
        val missingNetworks: Set<Network>,
        val requiredNetworks: Set<Network>,
        val availableNetworks: Set<Network>,
        val selectedNetworks: Set<Network>,
    )
}