package com.tangem.tangemtest.ucase.domain.paramsManager

import com.tangem.TangemSdk
import com.tangem.common.CompletionResult
import com.tangem.tangemtest._arch.structure.Id
import com.tangem.tangemtest._arch.structure.Payload
import com.tangem.tangemtest._arch.structure.PayloadHolder
import com.tangem.tangemtest._arch.structure.abstraction.Item
import com.tangem.tangemtest.ucase.domain.paramsManager.triggers.changeConsequence.ItemsChangeConsequence

typealias ActionResponse = CompletionResult<*>
typealias AffectedList = List<Item>
typealias AffectedItemsCallback = (AffectedList) -> Unit
typealias ActionCallback = (ActionResponse, AffectedList) -> Unit

/**
[REDACTED_AUTHOR]
 */
interface ItemsManager : PayloadHolder {

    fun itemChanged(id: Id, value: Any?, callback: AffectedItemsCallback? = null)
    fun setItems(items: List<Item>)
    fun getItems(): List<Item>
    fun setItemChangeConsequences(consequence: ItemsChangeConsequence?)
    fun invokeMainAction(tangemSdk: TangemSdk, callback: ActionCallback)
    fun getActionByTag(id: Id, tangemSdk: TangemSdk): ((ActionCallback) -> Unit)?
    fun attachPayload(payload: Payload)
    fun updateByItemList(list: List<Item>)
}

interface PayloadKey {
    companion object {
        val cardConfig: String = "cardConfig"
        val incomingJson: String = "incomingJson"
        val card = "card"
        val actionView = "actionView"
        val itemList = "itemList"
    }
}