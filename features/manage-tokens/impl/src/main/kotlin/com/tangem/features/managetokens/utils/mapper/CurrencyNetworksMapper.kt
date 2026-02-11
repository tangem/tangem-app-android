package com.tangem.features.managetokens.utils.mapper

import com.tangem.domain.managetokens.model.ManagedCryptoCurrency
import com.tangem.domain.managetokens.model.ManagedCryptoCurrency.SourceNetwork
import com.tangem.domain.models.network.Network
import com.tangem.features.managetokens.entity.item.CurrencyItemUM.Basic.NetworksUM
import com.tangem.features.managetokens.entity.item.CurrencyNetworkUM
import com.tangem.features.managetokens.utils.list.CurrencyUpdates
import com.tangem.features.managetokens.utils.ui.getIconRes
import kotlinx.collections.immutable.toImmutableList

internal fun ManagedCryptoCurrency.Token.toUiNetworksModel(
    isExpanded: Boolean,
    isItemsEditable: Boolean,
    updates: CurrencyUpdates,
    onSelectedStateChange: (SourceNetwork, Boolean) -> Unit,
    onLongTap: (SourceNetwork) -> Unit,
): NetworksUM {
    return if (isExpanded) {
        NetworksUM.Expanded(
            networks = availableNetworks.map { sourceNetwork ->
                val isSelectedByDefault = sourceNetwork.network in addedIn
                val isSelected = when (sourceNetwork.network) {
                    in updates.toAdd[this].orEmpty() -> true
                    in updates.toRemove[this].orEmpty() -> false
                    else -> isSelectedByDefault
                }
                sourceNetwork.toCurrencyNetworkModel(
                    isSelected = isSelected,
                    isEditable = isItemsEditable,
                    onSelectedStateChange = onSelectedStateChange,
                    onLongTap = onLongTap,
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
        currencySymbol = currencySymbol,
        onLongClick = {},
        isMainNetwork = false,
        onSelectedStateChange = onSelectedStateChange,
    )
}

internal fun SourceNetwork.toCurrencyNetworkModel(
    isSelected: Boolean,
    isEditable: Boolean,
    onSelectedStateChange: (SourceNetwork, Boolean) -> Unit,
    onLongTap: (SourceNetwork) -> Unit,
): CurrencyNetworkUM {
    return CurrencyNetworkUM(
        network = network,
        name = network.name.uppercase(),
        iconResId = id.getIconRes(isColored = isSelected || !isEditable),
        isSelected = isSelected || !isEditable,
        type = typeName,
        currencySymbol = network.currencySymbol,
        onLongClick = { onLongTap(this) },
        isMainNetwork = this is SourceNetwork.Main,
        onSelectedStateChange = { selected ->
            onSelectedStateChange(this, selected)
        },
    )
}