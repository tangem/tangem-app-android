package com.tangem.tangemtest._arch.structure.abstraction

import com.tangem.tangemtest._arch.structure.DataHolder
import com.tangem.tangemtest._arch.structure.Id
import com.tangem.tangemtest._arch.structure.Payload
import com.tangem.tangemtest._arch.structure.PayloadHolder

/**
[REDACTED_AUTHOR]
 */
interface Item : PayloadHolder {
    val id: Id
    var parent: Item?
}

abstract class BaseItem<D>(
        override var viewModel: ItemViewModel<D>
) : Item, DataHolder<ItemViewModel<D>> {
    override var parent: Item? = null
    override val payload: Payload = mutableMapOf()

    fun getData(): D? = viewModel.data

    fun setData(value: D?) {
        viewModel.data = value
    }

    fun restoreDefaultData(){
        setData(viewModel.defaultData)
    }
}