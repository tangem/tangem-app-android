package com.tangem.tangemtest._arch.structure.abstraction

import com.tangem.tangemtest._arch.structure.ILog
import com.tangem.tangemtest._arch.structure.Payload
import com.tangem.tangemtest._arch.structure.PayloadHolder


/**
[REDACTED_AUTHOR]
 */
typealias ValueChange<V> = (V?) -> Unit
typealias SafeValueChange<V> = (V) -> Unit

class KeyValue(val key: String, val value: Any)

class ViewState(
        var isHidden: Boolean = false,
        var backgroundColor: Int? = -1
) {

    var descriptionVisibility: Int = 0x00000008
        set(value) {
            field = value
            onDescriptionVisibilityChanged?.invoke(value)
        }

    var onDescriptionVisibilityChanged: SafeValueChange<Int>? = null
}

interface ItemViewModel : PayloadHolder {
    val viewState: ViewState
    var data: Any?
    var defaultData: Any?
    var onDataUpdated: ValueChange<Any?>?

    fun updateDataByView(data: Any?)
}

open class BaseItemViewModel(
        value: Any? = null,
        override val viewState: ViewState = ViewState()
) : ItemViewModel {

    override val payload: Payload = mutableMapOf()

    // Don't update it directly from a View. Use for it updateDataByView()
    override var data: Any? = value
        set(value) {
            if (handleDataUpdates(value)) field = value
        }

    // Data for restoring initial value
    override var defaultData: Any? = value
        set(value) {
            field = value
            data = value
        }

    // Use it for handling data updates in View
    override var onDataUpdated: ValueChange<Any?>? = null

    // When data updates directly it invokes onDataUpdated
    // return true = data will update
    // return false = data won't update
    protected open fun handleDataUpdates(value: Any?): Boolean {
        ILog.d(this, "handleDateUpdates: $value")
        onDataUpdated?.invoke(value)
        return true
    }

    // Use it to update the data from a View. It disables onDataUpdated to prevent a callback loop
    override fun updateDataByView(data: Any?) {
        ILog.d(this, "data changed: $data")
        val callback = onDataUpdated
        onDataUpdated = null
        this.data = data
        onDataUpdated = callback
    }
}

class ListViewModel(
        val itemList: List<KeyValue>,
        var selectedItem: Any?,
        override val viewState: ViewState = ViewState()
) : BaseItemViewModel(selectedItem)