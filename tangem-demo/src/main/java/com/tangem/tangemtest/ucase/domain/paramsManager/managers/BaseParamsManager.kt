package com.tangem.tangemtest.ucase.domain.paramsManager.managers

import com.tangem.CardManager
import com.tangem.tangemtest._arch.structure.Id
import com.tangem.tangemtest._arch.structure.abstraction.Item
import com.tangem.tangemtest.ucase.domain.actions.AttrForAction
import com.tangem.tangemtest.ucase.domain.actions.CardAction
import com.tangem.tangemtest.ucase.domain.paramsManager.ActionCallback
import com.tangem.tangemtest.ucase.domain.paramsManager.AffectedParamsCallback
import com.tangem.tangemtest.ucase.domain.paramsManager.ParamsManager
import com.tangem.tangemtest.ucase.domain.paramsManager.findDataParameter
import com.tangem.tangemtest.ucase.domain.paramsManager.triggers.changeConsequence.ParamsChangeConsequence

/**
[REDACTED_AUTHOR]
 */
abstract class BaseParamsManager(protected val action: CardAction) : ParamsManager {

    protected val paramsList: List<Item> by lazy { createParamsList() }

    abstract fun createParamsList(): List<Item>

    override fun parameterChanged(id: Id, value: Any?, callback: AffectedParamsCallback?) {
        if (paramsList.isEmpty()) return
        val foundParam = paramsList.findDataParameter(id) ?: return

        foundParam.viewModel.data = value
        applyChangesByAffectedParams(foundParam, callback)
    }

    override fun getParams(): MutableList<Item> = paramsList.toMutableList()

    override fun getActionByTag(id: Id, cardManager: CardManager): ((ActionCallback) -> Unit)? = null

    // Use it if current parameter needs to be affect any other parameters
    protected open fun applyChangesByAffectedParams(param: Item, callback: AffectedParamsCallback?) {
        getChangeConsequence()?.affectChanges(param, paramsList)?.let { callback?.invoke(it) }
    }

    protected open fun getChangeConsequence(): ParamsChangeConsequence? = null

    protected fun getAttrsForAction(cardManager: CardManager)
            : AttrForAction = AttrForAction(cardManager, paramsList, getChangeConsequence())
}