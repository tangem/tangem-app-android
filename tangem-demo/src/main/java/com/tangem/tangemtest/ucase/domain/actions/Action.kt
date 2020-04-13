package com.tangem.tangemtest.ucase.domain.actions

import com.tangem.TangemSdk
import com.tangem.common.CompletionResult
import com.tangem.tangemtest._arch.structure.Id
import com.tangem.tangemtest._arch.structure.Payload
import com.tangem.tangemtest._arch.structure.PayloadHolder
import com.tangem.tangemtest._arch.structure.abstraction.Item
import com.tangem.tangemtest.ucase.domain.paramsManager.ActionCallback
import com.tangem.tangemtest.ucase.domain.paramsManager.triggers.afterAction.AfterActionModification
import com.tangem.tangemtest.ucase.domain.paramsManager.triggers.changeConsequence.ItemsChangeConsequence

/**
[REDACTED_AUTHOR]
 *
 * The Card Action class family is designed for calling Card Manager functions
 * and then processing the response. It also allows you to extract the main action as a lambda expression
 */
data class AttrForAction(
        val tangemSdk: TangemSdk,
        val itemList: List<Item>,
        val payload: Payload,
        val consequence: ItemsChangeConsequence?
)

interface Action {
    fun executeMainAction(payload: PayloadHolder, attrs: AttrForAction, callback: ActionCallback)
    fun getActionByTag(payload: PayloadHolder, id: Id, attrs: AttrForAction): ((ActionCallback) -> Unit)? = null
}

abstract class BaseAction : Action {
    protected open fun handleResult(
            payload: PayloadHolder,
            commandResult: CompletionResult<*>,
            modifier: AfterActionModification?,
            attrs: AttrForAction,
            callback: ActionCallback
    ) {
        val modifiedItems = mutableListOf<Item>()
        modifier?.modify(payload, commandResult, attrs.itemList)?.forEach { item ->
            modifiedItems.add(item)
            attrs.consequence?.affectChanges(payload, item, attrs.itemList)?.let { modifiedItems.addAll(it) }
        }
        callback(commandResult, modifiedItems)
    }

    override fun getActionByTag(payload: PayloadHolder, id: Id, attrs: AttrForAction): ((ActionCallback) -> Unit)? = null
}