package com.tangem.tangemtest.cardUseCase.domain.paramsManager

import com.tangem.CardManager
import com.tangem.common.tlv.TlvTag
import com.tangem.tangemtest.cardUseCase.domain.actions.AttrForAction
import com.tangem.tangemtest.cardUseCase.domain.actions.CardAction
import com.tangem.tangemtest.cardUseCase.domain.paramsManager.modifiers.ParamsChangeConsequence

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