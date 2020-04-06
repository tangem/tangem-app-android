package com.tangem.tangemtest._arch.structure.abstraction

import com.tangem.tangemtest._arch.structure.Id

/**
[REDACTED_AUTHOR]
 */
fun List<Item>.findDataItem(id: Id): BaseItem<Any?>? {
    var foundItem: BaseItem<Any?>? = null
    iterate {
        if (it.id == id) {
            foundItem = it as? BaseItem<Any?>
            return@iterate
        }
    }
    return foundItem
}

fun List<Item>.iterate(func: (Item) -> Unit) {
    forEach {
        when (it) {
            is BaseItem<*> -> func(it)
            is Block -> it.itemList.iterate(func)
        }
    }
}