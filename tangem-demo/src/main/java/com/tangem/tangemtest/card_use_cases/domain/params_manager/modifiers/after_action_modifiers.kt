package com.tangem.tangemtest.card_use_cases.domain.params_manager.modifiers

import com.tangem.common.tlv.TlvTag
import com.tangem.tangemtest.card_use_cases.domain.params_manager.IncomingParameter
import com.tangem.tangemtest.card_use_cases.domain.params_manager.findParameter
import com.tangem.tasks.ScanEvent
import com.tangem.tasks.TaskEvent

/**
* [REDACTED_AUTHOR]
 *
 * The After Action Modification class family is intended for modifying parameters (if necessary)
 * after calling CardManager.anyAction.
 * Returns a list of parameters that have been modified
 */
interface AfterActionModification {
    fun modify(taskEvent: TaskEvent<*>, paramsList: List<IncomingParameter>): List<IncomingParameter>
}

class AfterScanModifier : AfterActionModification {
    override fun modify(taskEvent: TaskEvent<*>, paramsList: List<IncomingParameter>): List<IncomingParameter> {
        val parameter = paramsList.findParameter(TlvTag.CardId) ?: return listOf()

        return if (taskEvent is TaskEvent.Event && taskEvent.data is ScanEvent.OnReadEvent) {
            parameter.data = (taskEvent.data as ScanEvent.OnReadEvent).card.cardId
            listOf(parameter)
        } else listOf()
    }

}
