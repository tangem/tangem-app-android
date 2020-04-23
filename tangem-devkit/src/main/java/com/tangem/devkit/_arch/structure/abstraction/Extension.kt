package com.tangem.devkit._arch.structure.abstraction

import com.tangem.devkit._arch.structure.Id

/**
* [REDACTED_AUTHOR]
 */
fun List<Item>.findItem(id: Id): Item? {
    var foundItem: Item? = null
    iterate {
        if (it.id == id) {
            foundItem = it
            return@iterate
        }
    }
    return foundItem
}

fun List<Item>.iterate(func: (Item) -> Unit) {
    forEach {
        when (it) {
            is BaseItem -> func(it)
            is ItemGroup -> {
                func(it)
                it.itemList.iterate(func)
            }
        }
    }
}