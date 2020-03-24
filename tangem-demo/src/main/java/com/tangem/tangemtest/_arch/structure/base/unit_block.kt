package com.tangem.tangemtest._arch.structure.base

/**
[REDACTED_AUTHOR]
 */
typealias BlockModified = () -> Unit

interface Block : Unit {
    val unitList: MutableList<Unit>
    var blockModified: BlockModified?
}

abstract class BaseBlock : Block {
    override var parent: Unit? = null
    override val unitList: MutableList<Unit> = mutableListOf()
    override val payload: MutableMap<String, Any?> = mutableMapOf()
    override var blockModified: BlockModified? = null
}

open class ListUnitBlock(
        override val id: Id
) : BaseBlock(), ItemListHolder<Unit> {

    override fun setItems(list: MutableList<Unit>) {
        ULog.d(this, "setItems into: $id, count: ${list.size}")
        unitList.clear()
        unitList.addAll(list)
        unitList.forEach { it.parent = this }
        blockModified?.invoke()
    }

    override fun getItems(): MutableList<Unit> {
        return unitList
    }

    override fun addItem(item: Unit) {
        ULog.d(this, "addIte into: $id, who: ${item.id}")
        item.parent = this
        unitList.add(item)
        blockModified?.invoke()
    }

    override fun removeItem(item: Unit) {
        ULog.d(this, "removeItem from: $id, which: ${item.id}")
        unitList.remove(item)
        blockModified?.invoke()
    }

    override fun clear() {
        ULog.d(this, "clear $id")
        unitList.clear()
        blockModified?.invoke()
    }
}