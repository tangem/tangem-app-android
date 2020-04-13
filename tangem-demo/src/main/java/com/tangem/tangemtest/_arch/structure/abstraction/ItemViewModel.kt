package com.tangem.tangemtest._arch.structure.abstraction

import com.tangem.tangemtest._arch.structure.ILog
import com.tangem.tangemtest._arch.structure.Payload
import com.tangem.tangemtest._arch.structure.PayloadHolder


/**
[REDACTED_AUTHOR]
 */
typealias ValueChanged<V> = (V?) -> Unit
typealias SafeValueChanged<V> = (V) -> Unit

class KeyValue(val key: String, val value: Any)

class ViewState(
        isVisible: Boolean? = null,
        bgColor: Int? = -1
) : UpdateBy<ViewState> {

    class State<T>(
            stateValue: T,
            var onValueChanged: SafeValueChanged<T>? = null
    ) {
        var value = stateValue
            set(value) {
                if (preventSameChanges && field == value) return

                field = value
                onValueChanged?.invoke(value)
            }

        internal var preventSameChanges = true
    }

    var isVisibleState = State(isVisible)
    var backgroundColor = State(bgColor)
    var descriptionVisibility = State(0x00000008)

    internal fun preventSameChanges(isPrevented: Boolean) {
        val states = listOf(isVisibleState, backgroundColor, descriptionVisibility)
        states.forEach { it.preventSameChanges = isPrevented }
    }

    override fun update(value: ViewState) {
        isVisibleState = value.isVisibleState
        backgroundColor = value.backgroundColor
        descriptionVisibility = value.descriptionVisibility
    }
}

interface ItemViewModel : PayloadHolder, UpdateBy<ItemViewModel> {
    val viewState: ViewState
    var data: Any?
    var defaultData: Any?
    var onDataUpdated: ValueChanged<Any?>?

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
    override var onDataUpdated: ValueChanged<Any?>? = null

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

    override fun update(value: ItemViewModel) {
        viewState.update(value.viewState)
        defaultData = value.defaultData
        data = value.data
        payload.clear()
        payload.putAll(value.payload)
    }
}

class ListViewModel(
        val itemList: List<KeyValue>,
        var selectedItem: Any?,
        override val viewState: ViewState = ViewState()
) : BaseItemViewModel(selectedItem)