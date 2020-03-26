package com.tangem.tangemtest.ucase.domain.paramsManager.triggers.afterAction

import com.tangem.common.tlv.TlvTag
import com.tangem.tangemtest.ucase.domain.paramsManager.IncomingParameter
import com.tangem.tangemtest.ucase.domain.paramsManager.findParameter
import com.tangem.tasks.ScanEvent
import com.tangem.tasks.TaskEvent

/**
[REDACTED_AUTHOR]
 */
class AfterScanModifier : AfterActionModification {
    override fun modify(taskEvent: TaskEvent<*>, paramsList: List<IncomingParameter>): List<IncomingParameter> {
        val parameter = paramsList.findParameter(TlvTag.CardId) ?: return listOf()

        return if (taskEvent is TaskEvent.Event && taskEvent.data is ScanEvent.OnReadEvent) {
            parameter.data = (taskEvent.data as ScanEvent.OnReadEvent).card.cardId
            listOf(parameter)
        } else listOf()
    }

}