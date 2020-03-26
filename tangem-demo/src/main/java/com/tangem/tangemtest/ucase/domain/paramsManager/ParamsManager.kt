package com.tangem.tangemtest.ucase.domain.paramsManager

import com.tangem.CardManager
import com.tangem.tangemtest._arch.structure.Id
import com.tangem.tangemtest._arch.structure.abstraction.BaseItem
import com.tangem.tangemtest._arch.structure.abstraction.Item
import com.tangem.tasks.TaskEvent

typealias ActionResponse = TaskEvent<*>
typealias AffectedList = List<Item>
typealias AffectedParamsCallback = (AffectedList) -> Unit
typealias ActionCallback = (ActionResponse, AffectedList) -> Unit

/**
[REDACTED_AUTHOR]
 */
interface ParamsManager {

    fun parameterChanged(id: Id, value: Any?, callback: AffectedParamsCallback? = null)
    fun getParams(): List<Item>
    fun invokeMainAction(cardManager: CardManager, callback: ActionCallback)
    fun getActionByTag(id: Id, cardManager: CardManager): ((ActionCallback) -> Unit)?

}


fun List<Item>.findParameter(id: Id): Item? {
    return firstOrNull { it.id == id }
}

fun List<Item>.findDataParameter(id: Id): BaseItem<Any?>? {
    val item = firstOrNull { it.id == id }
    return if (item is BaseItem<*>) item as BaseItem<Any?>
    else null
}