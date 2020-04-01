package com.tangem.tangemtest.ucase.domain.paramsManager.triggers.afterAction

import com.tangem.tangemtest._arch.structure.PayloadHolder
import com.tangem.tangemtest._arch.structure.abstraction.Item
import com.tangem.tangemtest.ucase.domain.paramsManager.PayloadKey
import com.tangem.tangemtest.ucase.domain.paramsManager.findDataItem
import com.tangem.tangemtest.ucase.variants.TlvId
import com.tangem.tasks.ScanEvent
import com.tangem.tasks.TaskEvent

/**
[REDACTED_AUTHOR]
 */
class AfterScanModifier : AfterActionModification {
    override fun modify(payload: PayloadHolder, taskEvent: TaskEvent<*>, itemList: List<Item>): List<Item> {
        val foundItem = itemList.findDataItem(TlvId.CardId) ?: return listOf()

        return if (taskEvent is TaskEvent.Event && taskEvent.data is ScanEvent.OnReadEvent) {
            val card = (taskEvent.data as ScanEvent.OnReadEvent).card
            payload.set(PayloadKey.Card, card)
            foundItem.setData(card.cardId)
            listOf(foundItem)
        } else listOf()
    }

}