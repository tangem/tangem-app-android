package com.tangem.devkit.ucase.domain.paramsManager.triggers.changeConsequence

import com.tangem.devkit._arch.structure.PayloadHolder
import com.tangem.devkit._arch.structure.abstraction.Item
import com.tangem.devkit.ucase.domain.paramsManager.PayloadKey
import com.tangem.devkit.ucase.tunnel.ActionView
import com.tangem.devkit.ucase.variants.TlvId
import ru.dev.gbixahue.eu4d.lib.android.global.threading.postUI

/**
[REDACTED_AUTHOR]
 *
 * The ParamsChangeConsequence class family modifies parameters depending on the state
 * of the incoming parameter
 */
interface ItemsChangeConsequence {
    fun affectChanges(payload: PayloadHolder, changedItem: Item, itemList: List<Item>): List<Item>
}

class CardIdConsequence : ItemsChangeConsequence {

    override fun affectChanges(payload: PayloadHolder, changedItem: Item, itemList: List<Item>): List<Item> {
        val affectedList = mutableListOf<Item>()
        if (changedItem.id != TlvId.CardId) return affectedList

        val cidDataIsNull = changedItem.getData<String?>() == null
        postUI { (payload.get(PayloadKey.actionView) as? ActionView)?.enableActionFab(!cidDataIsNull) }
        return affectedList
    }
}