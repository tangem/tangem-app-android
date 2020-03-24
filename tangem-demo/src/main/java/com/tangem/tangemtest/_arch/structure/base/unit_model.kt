package com.tangem.tangemtest._arch.structure.base


/**
[REDACTED_AUTHOR]
 */
typealias ValueChange<V> = (V?) -> kotlin.Unit
typealias SafeValueChange<V> = (V) -> kotlin.Unit

interface UnitViewModel<D> : Payload {
    val viewState: ViewState
    var data: D?
    var onDataUpdated: ValueChange<D>?

    fun updateDataByView(data: D?)
}

open class BaseUnitViewModel<D> : UnitViewModel<D> {

    override val viewState: ViewState = ViewState()
    override val payload: MutableMap<String, Any?> = mutableMapOf()

    override var data: D? = null
        set(value) {
            if (handleDataUpdates(value)) field = value
        }

    override var onDataUpdated: ValueChange<D>? = null

    protected open fun handleDataUpdates(value: D?): Boolean {
        ULog.d(this, "handleDateUpdates: $value")
        onDataUpdated?.invoke(value)
        return true
    }

    override fun updateDataByView(data: D?) {
        ULog.d(this, "data changed: $data")
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