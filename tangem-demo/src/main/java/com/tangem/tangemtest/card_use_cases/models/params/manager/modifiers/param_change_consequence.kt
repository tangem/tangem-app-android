package com.tangem.tangemtest.card_use_cases.models.params.manager.modifiers

import com.tangem.common.tlv.TlvTag
import com.tangem.tangemtest.card_use_cases.models.params.manager.IncomingParameter

/**
[REDACTED_AUTHOR]
 */
interface ParamsChangeConsequence {
    fun affectChanges(changedParameter: IncomingParameter, paramsList: List<IncomingParameter>): List<IncomingParameter>
}

class ExampleConsequenceForCardId : ParamsChangeConsequence {
    override fun affectChanges(changedParameter: IncomingParameter, paramsList: List<IncomingParameter>): List<IncomingParameter> {
        if (changedParameter.tlvTag != TlvTag.CardId) return listOf()

        val affectedList = paramsList.filter { it.tlvTag != changedParameter.tlvTag }
        affectedList.forEach { it.data = changedParameter.data.hashCode() }
        return affectedList
    }
}