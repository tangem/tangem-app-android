package com.tangem.tangemtest.ucase.domain.actions

import com.tangem.CardManager
import com.tangem.common.tlv.TlvTag
import com.tangem.tangemtest.ucase.domain.paramsManager.ActionCallback
import com.tangem.tangemtest.ucase.domain.paramsManager.IncomingParameter
import com.tangem.tangemtest.ucase.domain.paramsManager.triggers.afterAction.AfterActionModification
import com.tangem.tangemtest.ucase.domain.paramsManager.triggers.changeConsequence.ParamsChangeConsequence
import com.tangem.tasks.TaskEvent

/**
[REDACTED_AUTHOR]
 *
 * The Card Action class family is designed for calling Card Manager functions
 * and then processing the response. It also allows you to extract the main action as a lambda expression
 */
data class AttrForAction(
        val cardManager: CardManager,
        val paramsList: List<IncomingParameter>,
        val consequence: ParamsChangeConsequence?
)

interface CardAction {
    fun executeMainAction(attrs: AttrForAction, callback: ActionCallback)
    fun getActionByTag(tag: TlvTag, attrs: AttrForAction): ((ActionCallback) -> Unit)? = null
}

abstract class BaseCardAction : CardAction {
    protected open fun handleResponse(
            taskEvent: TaskEvent<*>,
            modifier: AfterActionModification?,
            attrs: AttrForAction,
            callback: ActionCallback
    ) {
        val allModifiedParams = mutableListOf<IncomingParameter>()
        modifier?.modify(taskEvent, attrs.paramsList)?.forEach { parameter ->
            allModifiedParams.add(parameter)
            attrs.consequence?.affectChanges(parameter, attrs.paramsList)?.let { allModifiedParams.addAll(it) }
        }
        callback(taskEvent, allModifiedParams)
    }
}