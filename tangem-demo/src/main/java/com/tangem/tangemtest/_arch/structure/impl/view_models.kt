package com.tangem.tangemtest._arch.structure.impl

import com.tangem.tangemtest._arch.structure.base.BaseUnitViewModel

/**
[REDACTED_AUTHOR]
 */
open class TransitiveViewModel<D>(value: D?) : BaseUnitViewModel<D>() {
    init {
        data = value
    }
}

class StringViewModel(value: String?) : TransitiveViewModel<String>(value)
class NumberViewModel(value: Number?) : TransitiveViewModel<Number>(value)
class BoolViewModel(value: Boolean?) : TransitiveViewModel<Boolean>(value)
class ListViewModel(value: ListValueWrapper?) : TransitiveViewModel<ListValueWrapper>(value) {

    override fun handleDataUpdates(value: ListValueWrapper?): Boolean {
        val newValue = value ?: return false
        if (newValue.selectedItem == data?.selectedItem) return false

        onDataUpdated?.invoke(value)
        return true
    }
}

class KeyValue(val key: String, val value: Any)
class ListValueWrapper(var selectedItem: Any, val itemList: List<KeyValue>)