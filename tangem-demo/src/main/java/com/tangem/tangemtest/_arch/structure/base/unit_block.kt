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

open class ListUnitBlock : BaseBlock(), ItemListHolder<Unit> {

    override fun setItems(list: MutableList<Unit>) {
        ULog.d(this, "setUnits: ${list.size}")
        unitList.clear()
        unitList.addAll(list)
        unitList.forEach { it.parent = this }
        blockModified?.invoke()
    }

    override fun getItems(): MutableList<Unit> {
        ULog.d(this, "getUnits: ${unitList.size}")
        return unitList
    }

    override fun addItem(item: Unit) {
        ULog.d(this, "addUnit: ${unitList.size}")
        item.parent = this
        unitList.add(item)
        blockModified?.invoke()
    }

    override fun removeItem(item: Unit) {
        ULog.d(this, "removeUnit $item")
        unitList.remove(item)
        blockModified?.invoke()
    }

    override fun clear() {
        ULog.d(this, "clear")
        unitList.clear()
        blockModified?.invoke()
    }
}