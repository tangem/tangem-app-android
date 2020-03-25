package com.tangem.tangemtest._arch.structure.abstraction

import com.tangem.tangemtest._arch.structure.ILog
import com.tangem.tangemtest._arch.structure.Payload


/**
[REDACTED_AUTHOR]
 */
typealias ValueChange<V> = (V?) -> Unit
typealias SafeValueChange<V> = (V) -> Unit

interface ItemViewModel<D> : Payload {
    val viewState: ViewState
    var data: D?
    var onDataUpdated: ValueChange<D>?

    fun updateDataByView(data: D?)
}

open class BaseItemViewModel<D> : ItemViewModel<D> {

    override val viewState: ViewState = ViewState()
    override val payload: MutableMap<String, Any?> = mutableMapOf()

    override var data: D? = null
        set(value) {
            if (handleDataUpdates(value)) field = value
        }

    override var onDataUpdated: ValueChange<D>? = null

    protected open fun handleDataUpdates(value: D?): Boolean {
        ILog.d(this, "handleDateUpdates: $value")
        onDataUpdated?.invoke(value)
        return true
    }

    override fun updateDataByView(data: D?) {
        ILog.d(this, "data changed: $data")
        val callback = onDataUpdated
        onDataUpdated = null
        this.data = data
        onDataUpdated = callback
    }
}

class ViewState {
    var descriptionVisibility: Int = 0x00000008
        set(value) {
            field = value
            onDescriptionVisibilityChanged?.invoke(value)
        }

    var onDescriptionVisibilityChanged: SafeValueChange<Int>? = null
}