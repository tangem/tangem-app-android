package com.tangem.features.managetokens.utils.list

import com.tangem.domain.managetokens.model.ManagedCryptoCurrency
import com.tangem.domain.tokens.model.Network
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.update

internal typealias ChangedCurrencies = Map<ManagedCryptoCurrency.ID, Set<Network.ID>>

internal interface ChangedCurrenciesManager {

    val currenciesToAdd: MutableStateFlow<ChangedCurrencies>
    val currenciesToRemove: MutableStateFlow<ChangedCurrencies>

    fun updateChangedItems(
        currencyId: ManagedCryptoCurrency.ID,
        networkId: Network.ID,
        removeFromIfPresent: MutableStateFlow<ChangedCurrencies>,
        addToIfNotPresent: MutableStateFlow<ChangedCurrencies>,
    ) {
        val present = removeFromIfPresent.value[currencyId].orEmpty()

        if (networkId in present) {
            removeFromIfPresent.update { items ->
                items.toMutableMap().apply {
                    val ids = present - networkId

                    if (ids.isEmpty()) {
                        remove(currencyId)
                    } else {
                        set(currencyId, ids)
                    }
                }
            }
        } else {
            addToIfNotPresent.update { items ->
                val alreadyAdded = items[currencyId] ?: emptySet()
                if (networkId in alreadyAdded) {
                    return@update items
                }

                items + (currencyId to alreadyAdded + networkId)
            }
        }
    }
}
