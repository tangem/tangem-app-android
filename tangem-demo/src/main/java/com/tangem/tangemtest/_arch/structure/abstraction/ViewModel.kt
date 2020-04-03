package com.tangem.tangemtest._arch.structure.abstraction

import com.tangem.tangemtest._arch.structure.ILog
import com.tangem.tangemtest._arch.structure.Payload
import com.tangem.tangemtest._arch.structure.PayloadHolder


/**
[REDACTED_AUTHOR]
 */
typealias ValueChange<V> = (V?) -> Unit
typealias SafeValueChange<V> = (V) -> Unit

interface ItemViewModel<D : Any?> : PayloadHolder {
    val viewState: ViewState
    var data: D?
    var defaultData: D?
    var onDataUpdated: ValueChange<D>?

    fun updateDataByView(data: D?)
}

open class BaseItemViewModel<D> : ItemViewModel<D> {

    override val viewState: ViewState = ViewState()
    override val payload: Payload = mutableMapOf()

    // Don't update it directly from a View. Use for it updateDataByView()
    override var data: D? = null
        set(value) {
            if (handleDataUpdates(value)) field = value
        }

    // Data for restoring initial value
    override var defaultData: D? = null
        set(value) {
            field = value
            data = value
        }

    // Use it for handling data updates in View
    override var onDataUpdated: ValueChange<D>? = null

    // When data updates directly it invokes onDataUpdated
    // return true = data will update
    // return false = data won't update
    protected open fun handleDataUpdates(value: D?): Boolean {
        ILog.d(this, "handleDateUpdates: $value")
        onDataUpdated?.invoke(value)
        return true
    }

    // Use it to update the data from a View. It disables onDataUpdated to prevent a callback loop
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