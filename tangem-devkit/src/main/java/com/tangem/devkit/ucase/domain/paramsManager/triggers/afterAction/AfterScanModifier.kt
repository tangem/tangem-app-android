package com.tangem.devkit.ucase.domain.paramsManager.triggers.afterAction

import com.tangem.commands.Card
import com.tangem.commands.CardStatus
import com.tangem.common.CompletionResult
import com.tangem.devkit._arch.structure.PayloadHolder
import com.tangem.devkit._arch.structure.abstraction.Item
import com.tangem.devkit._arch.structure.abstraction.findItem
import com.tangem.devkit.ucase.domain.paramsManager.PayloadKey
import com.tangem.devkit.ucase.tunnel.ActionView
import com.tangem.devkit.ucase.tunnel.CardError
import com.tangem.devkit.ucase.variants.TlvId

import ru.dev.gbixahue.eu4d.lib.android.global.threading.postUI

/**
* [REDACTED_AUTHOR]
 */
class AfterScanModifier : AfterActionModification {
    override fun modify(payload: PayloadHolder, commandResult: CompletionResult<*>, itemList: List<Item>): List<Item> {
        val foundItem = itemList.findItem(TlvId.CardId) ?: return listOf()
        val card = smartCast(commandResult) ?: return listOf()
        val actionView = payload.get(PayloadKey.actionView) as? ActionView ?: return listOf()

        return if (isNotPersonalized(card)) {
            actionView.showSnackbar(CardError.NotPersonalized)
            postUI { actionView.enableActionFab(false) }
            listOf()
        } else {
            payload.set(PayloadKey.card, card)
            foundItem.setData(card.cardId)
            postUI { actionView.enableActionFab(true) }
            listOf(foundItem)
        }
    }

    private fun smartCast(commandResult: CompletionResult<*>): Card? {
        return (commandResult as? CompletionResult.Success<Card>)?.data
    }

    private fun isNotPersonalized(card: Card): Boolean = card.status == CardStatus.NotPersonalized
}