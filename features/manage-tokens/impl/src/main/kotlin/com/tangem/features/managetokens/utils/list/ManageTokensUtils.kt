package com.tangem.features.managetokens.utils.list

import com.tangem.features.managetokens.entity.item.CurrencyItemUM
import kotlinx.collections.immutable.ImmutableList
import kotlinx.collections.immutable.toPersistentList

internal fun getLoadingItems(): ImmutableList<CurrencyItemUM> {
    return List(size = 10) { index ->
        CurrencyItemUM.Loading(index)
    }.toPersistentList()
}