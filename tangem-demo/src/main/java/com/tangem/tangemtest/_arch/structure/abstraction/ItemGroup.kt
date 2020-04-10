package com.tangem.tangemtest._arch.structure.abstraction

import com.tangem.tangemtest._arch.structure.ILog
import com.tangem.tangemtest._arch.structure.Id

/**
[REDACTED_AUTHOR]
 */
interface ItemGroup : Item {
    val itemList: MutableList<Item>

    fun setItems(list: MutableList<Item>)
    fun getItems(): MutableList<Item>
    fun addItem(item: Item)
    fun removeItem(item: Item)
    fun clear()
}

open class SimpleItemGroup(
        override val id: Id,
        override var viewModel: ItemViewModel = BaseItemViewModel()
) : ItemGroup {

    override var parent: Item? = null
    override val itemList: MutableList<Item> = mutableListOf()

    override fun setItems(list: MutableList<Item>) {
        ILog.d(this, "setItems into: $id, count: ${list.size}")
        itemList.forEach { it.removed(this) }
        itemList.clear()
        list.forEach { addItem(it) }
    }

    override fun getItems(): MutableList<Item> = itemList

    override fun addItem(item: Item) {
        ILog.d(this, "addItem into: $id, who: ${item.id}")
        itemList.add(item)
        item.added(this)
    }

    override fun removeItem(item: Item) {
        ILog.d(this, "removeItem from: $id, which: ${item.id}")
        itemList.remove(item)
        item.removed(this)
    }

    override fun clear() {
        ILog.d(this, "clear $id")
        itemList.clear()
    }
}