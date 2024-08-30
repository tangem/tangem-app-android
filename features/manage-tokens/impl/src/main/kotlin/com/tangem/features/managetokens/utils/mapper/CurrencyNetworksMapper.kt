package com.tangem.features.managetokens.utils.mapper

import com.tangem.domain.managetokens.model.ManagedCryptoCurrency
import com.tangem.domain.managetokens.model.ManagedCryptoCurrency.SourceNetwork
import com.tangem.domain.tokens.model.Network
import com.tangem.features.managetokens.entity.item.CurrencyItemUM.Basic.NetworksUM
import com.tangem.features.managetokens.entity.item.CurrencyNetworkUM
import com.tangem.features.managetokens.utils.ui.getIconRes
import kotlinx.collections.immutable.toImmutableList

internal fun ManagedCryptoCurrency.Token.toUiNetworksModel(
    isExpanded: Boolean,
    isItemsEditable: Boolean,
    onSelectedStateChange: (SourceNetwork, Boolean) -> Unit,
): NetworksUM {
    return if (isExpanded) {
        NetworksUM.Expanded(
            networks = availableNetworks.map {
                it.toCurrencyNetworkModel(
                    isSelected = it.network in addedIn,
                    isEditable = isItemsEditable,
                    onSelectedStateChange = onSelectedStateChange,
                )
            }.toImmutableList(),
        )
    } else {
        NetworksUM.Collapsed
    }
}

internal fun Network.toCurrencyNetworkModel(
    isSelected: Boolean,
    onSelectedStateChange: (Boolean) -> Unit,
): CurrencyNetworkUM {
    return CurrencyNetworkUM(
        network = this,
        name = name,
        iconResId = id.getIconRes(isColored = true),
        isSelected = isSelected,
        type = standardType.name,
        isMainNetwork = false,
        onSelectedStateChange = onSelectedStateChange,
    )
}

private fun SourceNetwork.toCurrencyNetworkModel(
    isSelected: Boolean,
    isEditable: Boolean,
    onSelectedStateChange: (SourceNetwork, Boolean) -> Unit,
): CurrencyNetworkUM {
    return CurrencyNetworkUM(
        network = network,
        name = network.name.uppercase(),
        iconResId = id.getIconRes(isColored = isSelected || !isEditable),
        isSelected = isSelected || !isEditable,
        type = typeName,
        isMainNetwork = this is SourceNetwork.Main,
        onSelectedStateChange = { selected ->
            onSelectedStateChange(this, selected)
        },
    )
}