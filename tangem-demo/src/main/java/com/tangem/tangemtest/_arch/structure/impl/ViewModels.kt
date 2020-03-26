package com.tangem.tangemtest._arch.structure.impl

import com.tangem.tangemtest._arch.structure.abstraction.BaseItemViewModel

/**
[REDACTED_AUTHOR]
 */
open class TransitiveViewModel<D>(value: D?) : BaseItemViewModel<D>() {
    init {
        data = value
    }
}

class AnyViewModel(value: Any? = null) : TransitiveViewModel<Any>(value)
class StringViewModel(value: String? = null) : TransitiveViewModel<String>(value)
class NumberViewModel(value: Number? = null) : TransitiveViewModel<Number>(value)
class BoolViewModel(value: Boolean? = null) : TransitiveViewModel<Boolean>(value)
class ListViewModel(value: ListValueWrapper? = null) : TransitiveViewModel<ListValueWrapper>(value) {

    override fun handleDataUpdates(value: ListValueWrapper?): Boolean {
        val newValue = value ?: return false
        if (newValue.selectedItem == data?.selectedItem) return false

        onDataUpdated?.invoke(value)
        return true
    }
}

class KeyValue(val key: String, val value: Any)
class ListValueWrapper(var selectedItem: Any?, val itemList: List<KeyValue>)