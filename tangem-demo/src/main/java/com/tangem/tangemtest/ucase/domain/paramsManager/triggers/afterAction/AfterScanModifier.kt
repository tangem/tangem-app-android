package com.tangem.tangemtest.ucase.domain.paramsManager.triggers.afterAction

import com.tangem.commands.Card
import com.tangem.commands.CardStatus
import com.tangem.tangemtest._arch.structure.PayloadHolder
import com.tangem.tangemtest._arch.structure.abstraction.Item
import com.tangem.tangemtest.ucase.domain.paramsManager.PayloadKey
import com.tangem.tangemtest.ucase.domain.paramsManager.findDataItem
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

        return if (taskEvent is TaskEvent.Event && taskEvent.data is ScanEvent.OnReadEvent) {
            val card = (taskEvent.data as ScanEvent.OnReadEvent).card
            val actionView = payload.remove(PayloadKey.actionView) as? ActionView ?: return listOf()

            if (isPersonalized(card)) {
                actionView.showSnackbar(CardError.NotPersonalized)
                return listOf()
            }

            payload.set(PayloadKey.card, card)
            foundItem.setData(card.cardId)
            postUI { actionView.showActionFab(true) }
            listOf(foundItem)
        } else listOf()
    }

    private fun isPersonalized(card: Card): Boolean = card.status == CardStatus.NotPersonalized
}