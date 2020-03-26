package com.tangem.tangemtest.ucase.domain.paramsManager.triggers.changeConsequence

import com.tangem.tangemtest._arch.structure.abstraction.Item
import com.tangem.tangemtest.ucase.variants.TlvId

/**
[REDACTED_AUTHOR]
 *
 * The ParamsChangeConsequence class family modifies parameters depending on the state
 * of the incoming parameter
 */
interface ParamsChangeConsequence {
    fun affectChanges(changedParameter: Item, paramsList: List<Item>): List<Item>
}

class ExampleConsequenceForCardId : ParamsChangeConsequence {
    override fun affectChanges(changedParameter: Item, paramsList: List<Item>): List<Item> {
        if (changedParameter.id != TlvId.CardId) return listOf()

        val affectedList = paramsList.filter { it.id != changedParameter.id }
//        affectedList.forEach { it.viewModel.data = changedParameter.viewModel.data?.hashCode() }
        return affectedList
    }
}