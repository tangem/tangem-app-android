package com.tangem.features.managetokens.utils.ui

import com.tangem.domain.managetokens.model.ManagedCryptoCurrency
import com.tangem.domain.managetokens.model.ManagedCryptoCurrency.SourceNetwork
import com.tangem.features.managetokens.entity.CurrencyItemUM
import com.tangem.features.managetokens.entity.CurrencyItemUM.Basic.NetworksUM
import com.tangem.features.managetokens.utils.mapper.toUiNetworksModel
import kotlinx.collections.immutable.toImmutableList

internal fun CurrencyItemUM.toggleExpanded(
    currency: ManagedCryptoCurrency,
    isEditable: Boolean,
    onSelectCurrencyNetwork: (SourceNetwork, Boolean) -> Unit,
): CurrencyItemUM {
    if (currency !is ManagedCryptoCurrency.Token) return this

    return when (this) {
        is CurrencyItemUM.Custom -> this
        is CurrencyItemUM.Basic -> {
            val isExpanded = networks !is NetworksUM.Expanded

            copy(
                icon = icon.copySealed(
                    isGrayscale = if (isEditable) !currency.isAdded && !isExpanded else false,
                ),
                networks = currency.toUiNetworksModel(
                    isExpanded = isExpanded,
                    isItemsEditable = isEditable,
                    onSelectedStateChange = onSelectCurrencyNetwork,
                ),
            )
        }
    }
}

internal fun CurrencyItemUM.update(currency: ManagedCryptoCurrency): CurrencyItemUM {
    return when (this) {
        is CurrencyItemUM.Custom -> this
        is CurrencyItemUM.Basic -> {
            if (currency !is ManagedCryptoCurrency.Token) {
                return this
            }

            copy(
                icon = icon.copySealed(
                    isGrayscale = networks is NetworksUM.Collapsed && !currency.isAdded,
                ),
                networks = updateNetworks(currency),
            )
        }
    }
}

private fun CurrencyItemUM.Basic.updateNetworks(currency: ManagedCryptoCurrency.Token): NetworksUM = when (networks) {
    is NetworksUM.Collapsed -> networks
    is NetworksUM.Expanded -> networks.copy(
        networks = networks.networks.map { network ->
            val isSelected = network.id in currency.addedIn

            network.copy(
                iconResId = network.id.getIconRes(isSelected),
                isSelected = isSelected,
            )
        }.toImmutableList(),
    )
}
