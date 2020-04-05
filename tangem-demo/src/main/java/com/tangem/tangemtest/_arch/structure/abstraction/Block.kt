package com.tangem.tangemtest._arch.structure.abstraction

import com.tangem.tangemtest._arch.structure.ILog
import com.tangem.tangemtest._arch.structure.Id
import com.tangem.tangemtest._arch.structure.ItemListHolder

/**
[REDACTED_AUTHOR]
 */
interface Block : Item {
    val itemList: MutableList<Item>
}

abstract class BaseBlock : Block {
    override var parent: Item? = null
    override val itemList: MutableList<Item> = mutableListOf()
    override val payload: MutableMap<String, Any?> = mutableMapOf()
}

open class ListItemBlock(
        override val id: Id
) : BaseBlock(), ItemListHolder<Item> {

    override fun setItems(list: MutableList<Item>) {
        ILog.d(this, "setItems into: $id, count: ${list.size}")
        itemList.clear()
        itemList.addAll(list)
        itemList.forEach { it.parent = this }
    }

    override fun getItems(): MutableList<Item> {
        return itemList
    }

    override fun addItem(item: Item) {
        ILog.d(this, "addItem into: $id, who: ${item.id}")
        item.parent = this
        itemList.add(item)
    }

    override fun removeItem(item: Item) {
        ILog.d(this, "removeItem from: $id, which: ${item.id}")
        itemList.remove(item)
    }

    override fun clear() {
        ILog.d(this, "clear $id")
        itemList.clear()
    }
}