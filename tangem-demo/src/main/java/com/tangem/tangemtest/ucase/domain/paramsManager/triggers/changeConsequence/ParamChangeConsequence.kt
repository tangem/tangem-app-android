package com.tangem.tangemtest.ucase.domain.paramsManager.triggers.changeConsequence

import com.tangem.commands.Card
import com.tangem.commands.EllipticCurve
import com.tangem.common.extensions.calculateSha256
import com.tangem.common.extensions.calculateSha512
import com.tangem.tangemtest._arch.structure.PayloadHolder
import com.tangem.tangemtest._arch.structure.abstraction.Item
import com.tangem.tangemtest._arch.structure.abstraction.findDataItem
import com.tangem.tangemtest.ucase.domain.paramsManager.PayloadKey
import com.tangem.tangemtest.ucase.tunnel.ActionView
import com.tangem.tangemtest.ucase.variants.TlvId
import ru.dev.gbixahue.eu4d.lib.android.global.threading.postUI

/**
[REDACTED_AUTHOR]
 *
 * The ParamsChangeConsequence class family modifies parameters depending on the state
 * of the incoming parameter
 */
interface ItemsChangeConsequence {
    fun affectChanges(payload: PayloadHolder, changedItem: Item, itemList: List<Item>): List<Item>?
}

class SignScanConsequence: ItemsChangeConsequence {

    override fun affectChanges(payload: PayloadHolder, changedItem: Item, itemList: List<Item>): List<Item>? {
        if (changedItem.id != TlvId.CardId) return null

        val hashItem = itemList.findDataItem(TlvId.TransactionOutHash) ?: return null
        val affectedItems = mutableListOf(hashItem)
        val card = payload.remove(PayloadKey.card) as Card?
        if (card == null) {
            hashItem.restoreDefaultData()
            postUI { (payload.get(PayloadKey.actionView) as? ActionView)?.enableActionFab(false) }
        } else {
            val dataForHashing = hashItem.getData() as? String ?: "Any data mother...s"
            val hashedData = when(card.curve) {
                EllipticCurve.Secp256k1 -> dataForHashing.calculateSha256()
                EllipticCurve.Ed25519 -> dataForHashing.calculateSha512()
                else -> return null
            }
            hashItem.setData(hashedData)
        }
        return affectedItems.toList()
    }
}