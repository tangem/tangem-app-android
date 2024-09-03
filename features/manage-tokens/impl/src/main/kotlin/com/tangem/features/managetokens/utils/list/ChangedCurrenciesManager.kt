package com.tangem.features.managetokens.utils.list

import com.tangem.domain.managetokens.model.ManagedCryptoCurrency
import com.tangem.domain.tokens.model.Network
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.update

internal typealias ChangedCurrencies = Map<ManagedCryptoCurrency.Token, Set<Network>>

internal class ChangedCurrenciesManager {

    val currenciesToAdd: MutableStateFlow<ChangedCurrencies> = MutableStateFlow(emptyMap())
    val currenciesToRemove: MutableStateFlow<ChangedCurrencies> = MutableStateFlow(emptyMap())

    fun addCurrency(currency: ManagedCryptoCurrency.Token, network: Network) {
        updateChangedItems(currency, network, currenciesToRemove, currenciesToAdd)
    }

    fun removeCurrency(currency: ManagedCryptoCurrency.Token, network: Network) {
        updateChangedItems(currency, network, currenciesToAdd, currenciesToRemove)
    }

    fun containsCurrency(currency: ManagedCryptoCurrency.Token, network: Network): Boolean {
        return network in currenciesToAdd.value[currency].orEmpty() ||
            network in currenciesToRemove.value[currency].orEmpty()
    }

    private fun updateChangedItems(
        currency: ManagedCryptoCurrency.Token,
        network: Network,
        removeFromIfPresent: MutableStateFlow<ChangedCurrencies>,
        addToIfNotPresent: MutableStateFlow<ChangedCurrencies>,
    ) {
        val present = removeFromIfPresent.value[currency].orEmpty()

        if (network in present) {
            removeFromIfPresent.update { items ->
                items.toMutableMap().apply {
                    val ids = present - network

                    if (ids.isEmpty()) {
                        remove(currency)
                    } else {
                        set(currency, ids)
                    }
                }
            }
        } else {
            addToIfNotPresent.update { items ->
                val alreadyAdded = items[currency] ?: emptySet()
                if (network in alreadyAdded) {
                    return@update items
                }

                items + (currency to alreadyAdded + network)
            }
        }
    }
}