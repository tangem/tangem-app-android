package com.tangem.tangemtest.ucase.domain.paramsManager

import com.tangem.CardManager
import com.tangem.tangemtest._arch.structure.Id
import com.tangem.tangemtest._arch.structure.Payload
import com.tangem.tangemtest._arch.structure.PayloadHolder
import com.tangem.tangemtest._arch.structure.abstraction.BaseItem
import com.tangem.tangemtest._arch.structure.abstraction.Item
import com.tangem.tangemtest.ucase.domain.paramsManager.triggers.changeConsequence.ItemsChangeConsequence
import com.tangem.tasks.TaskEvent

typealias ActionResponse = TaskEvent<*>
typealias AffectedList = List<Item>
typealias AffectedItemsCallback = (AffectedList) -> Unit
typealias ActionCallback = (ActionResponse, AffectedList) -> Unit

/**
[REDACTED_AUTHOR]
 */
interface ItemsManager : PayloadHolder {

    fun itemChanged(id: Id, value: Any?, callback: AffectedItemsCallback? = null)
    fun getItems(): List<Item>
    fun invokeMainAction(cardManager: CardManager, callback: ActionCallback)
    fun getActionByTag(id: Id, cardManager: CardManager): ((ActionCallback) -> Unit)?
    fun attachPayload(payload: Payload)

    val consequence: ItemsChangeConsequence?
}

interface PayloadKey {
    companion object {
        val cardConfig: String = "cardConfig"
        val incomingJson: String = "incomingJson"
        val card = "card"
        val actionView = "actionView"
    }
}

fun List<Item>.findItem(id: Id): Item? {
    return firstOrNull { it.id == id }
}

fun List<Item>.findDataItem(id: Id): BaseItem<Any?>? {
    val item = firstOrNull { it.id == id }
    return if (item is BaseItem<*>) item as BaseItem<Any?>
    else null
}