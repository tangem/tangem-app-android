package com.tangem.tangemtest.card_use_cases.domain.params_manager

import com.tangem.CardManager
import com.tangem.common.tlv.TlvTag
import com.tangem.tangemtest.card_use_cases.domain.card_action.*
import com.tangem.tangemtest.card_use_cases.domain.params_manager.modifiers.ParamsChangeConsequence
import com.tangem.tasks.TaskEvent

/**
[REDACTED_AUTHOR]
 *
 * ParamsManager monitors parameter changes and runs the CardManager command
 */

typealias ActionResponse = TaskEvent<*>
typealias AffectedList = List<IncomingParameter>
typealias AffectedParamsCallback = (AffectedList) -> Unit
typealias ActionCallback = (ActionResponse, AffectedList) -> Unit

class IncomingParameter(val tlvTag: TlvTag, var data: Any? = null)

interface ParamsManager {

    fun parameterChanged(tag: TlvTag, value: Any?, callback: AffectedParamsCallback? = null)
    fun getParams(): List<IncomingParameter>
    fun invokeMainAction(cardManager: CardManager, callback: ActionCallback)
    fun getActionByTag(tag: TlvTag, cardManager: CardManager): ((ActionCallback) -> Unit)?

}

fun List<IncomingParameter>.findParameter(tag: TlvTag): IncomingParameter? {
    return firstOrNull { it.tlvTag == tag }
}

abstract class BaseParamsManager(protected val action: CardAction) : ParamsManager {

    protected val paramsList: List<IncomingParameter> by lazy { createParamsList() }

    abstract fun createParamsList(): List<IncomingParameter>

    override fun parameterChanged(tag: TlvTag, value: Any?, callback: AffectedParamsCallback?) {
        if (paramsList.isEmpty()) return
        val foundParam = paramsList.findParameter(tag) ?: return

        foundParam.data = value
        applyChangesByAffectedParams(foundParam, callback)
    }

    override fun getParams(): MutableList<IncomingParameter> = paramsList.toMutableList()

    override fun getActionByTag(tag: TlvTag, cardManager: CardManager): ((ActionCallback) -> Unit)? = null

    // Use it if current parameter needs to be affect any other parameters
    protected open fun applyChangesByAffectedParams(param: IncomingParameter, callback: AffectedParamsCallback?) {
        getChangeConsequence()?.affectChanges(param, paramsList)?.let { callback?.invoke(it) }
    }

    protected open fun getChangeConsequence(): ParamsChangeConsequence? = null

    protected fun getAttrsForAction(cardManager: CardManager)
            : AttrForAction = AttrForAction(cardManager, paramsList, getChangeConsequence())
}

class ScanParamsManager : BaseParamsManager(ScanAction()) {
    override fun createParamsList(): List<IncomingParameter> {
        return listOf(IncomingParameter(TlvTag.CardId, null))
    }

    override fun invokeMainAction(cardManager: CardManager, callback: ActionCallback) {
        action.executeMainAction(getAttrsForAction(cardManager), callback)
    }
}

class SignParamsManager : BaseParamsManager(SignAction()) {
    override fun createParamsList(): List<IncomingParameter> {
        return listOf(
                IncomingParameter(TlvTag.CardId, null),
                IncomingParameter(TlvTag.TransactionOutHash, "Data used for hashing")
        )
    }

    override fun invokeMainAction(cardManager: CardManager, callback: ActionCallback) {
        action.executeMainAction(getAttrsForAction(cardManager), callback)
    }

    override fun getActionByTag(tag: TlvTag, cardManager: CardManager): ((ActionCallback) -> Unit)? {
        return action.getActionByTag(tag, getAttrsForAction(cardManager))
    }
}

class PersonalizeParamsManager : BaseParamsManager(PersonalizeAction()) {
    override fun createParamsList(): List<IncomingParameter> {
        return listOf()
    }

    override fun invokeMainAction(cardManager: CardManager, callback: ActionCallback) {
        action.executeMainAction(getAttrsForAction(cardManager), callback)
    }
}

class DepersonalizeParamsManager : BaseParamsManager(DepesonalizeAction()) {
    override fun createParamsList(): List<IncomingParameter> {
        return listOf(IncomingParameter(TlvTag.CardId, null))
    }

    override fun invokeMainAction(cardManager: CardManager, callback: ActionCallback) {
        action.executeMainAction(getAttrsForAction(cardManager), callback)
    }

    override fun getActionByTag(tag: TlvTag, cardManager: CardManager): ((ActionCallback) -> Unit)? {
        return action.getActionByTag(tag, getAttrsForAction(cardManager))
    }
}