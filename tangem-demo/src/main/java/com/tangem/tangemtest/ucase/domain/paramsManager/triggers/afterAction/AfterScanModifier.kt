package com.tangem.tangemtest.ucase.domain.paramsManager.triggers.afterAction

import com.tangem.commands.Card
import com.tangem.commands.CardStatus
import com.tangem.tangemtest._arch.structure.PayloadHolder
import com.tangem.tangemtest._arch.structure.abstraction.Item
import com.tangem.tangemtest._arch.structure.abstraction.findDataItem
import com.tangem.tangemtest.ucase.domain.paramsManager.PayloadKey
import com.tangem.tangemtest.ucase.tunnel.ActionView
import com.tangem.tangemtest.ucase.tunnel.CardError
import com.tangem.tangemtest.ucase.variants.TlvId
import com.tangem.tasks.ScanEvent
import com.tangem.tasks.TaskEvent
import ru.dev.gbixahue.eu4d.lib.android.global.threading.postUI

/**
[REDACTED_AUTHOR]
 */
class AfterScanModifier : AfterActionModification {
    override fun modify(payload: PayloadHolder, taskEvent: TaskEvent<*>, itemList: List<Item>): List<Item> {
        val foundItem = itemList.findDataItem(TlvId.CardId) ?: return listOf()
        val card = smartCast(taskEvent)?.card ?: return listOf()
        val actionView = payload.get(PayloadKey.actionView) as? ActionView ?: return listOf()

        return if (isNotPersonalized(card)) {
            actionView.showSnackbar(CardError.NotPersonalized)
            postUI { actionView.showActionFab(false) }
            listOf()
        } else {
            payload.set(PayloadKey.card, card)
            foundItem.setData(card.cardId)
            postUI { actionView.showActionFab(true) }
            listOf(foundItem)
        }
    }

    private fun smartCast(taskEvent: TaskEvent<*>): ScanEvent.OnReadEvent? {
        return (taskEvent as? TaskEvent.Event)?.data as? ScanEvent.OnReadEvent
    }

    private fun isNotPersonalized(card: Card): Boolean = card.status == CardStatus.NotPersonalized
}