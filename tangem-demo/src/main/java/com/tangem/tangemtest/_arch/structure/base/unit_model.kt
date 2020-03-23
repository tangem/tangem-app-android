package com.tangem.tangemtest._arch.structure.base


/**
[REDACTED_AUTHOR]
 */
data class ViewState(
        var isEnabled: Boolean = true,
        var visibility: Int = 0x00000000,
        var descriptionVisibility: Int = 0x00000008
)

interface UnitViewModel<D> : Payload {
    var viewState: ViewState?
    var data: D?

    fun updateData(data: D?)
}

open class BaseUnitViewModel<D>(
        override var data: D? = null
) : UnitViewModel<D> {

    override var viewState: ViewState? = ViewState()
    override val payload: MutableMap<String, Any?> = mutableMapOf()

    override fun updateData(data: D?) {
        ULog.d(this, "data changed: $data")
        this.data = data
    }
}