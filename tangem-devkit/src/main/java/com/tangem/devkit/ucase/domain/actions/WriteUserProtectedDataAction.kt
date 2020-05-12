package com.tangem.devkit.ucase.domain.actions

import com.tangem.devkit._arch.structure.Id
import com.tangem.devkit._arch.structure.PayloadHolder
import com.tangem.devkit._arch.structure.abstraction.findItem
import com.tangem.devkit.ucase.domain.paramsManager.ActionCallback
import com.tangem.devkit.ucase.variants.TlvId
import ru.dev.gbixahue.eu4d.lib.kotlin.stringOf


class WriteUserProtectedDataAction : BaseAction() {
    override fun executeMainAction(payload: PayloadHolder, attrs: AttrForAction, callback: ActionCallback) {
        val protectedUserData = (attrs.itemList.findItem(TlvId.ProtectedUserData)?.getData() as? String)?.toByteArray()
                ?: return
        val cardId = attrs.itemList.findItem(TlvId.CardId)?.viewModel?.data ?: return
        val counter = (attrs.itemList.findItem(TlvId.Counter)?.viewModel?.data as? Int) ?: 1

        attrs.tangemSdk.writeProtectedUserData(stringOf(cardId), protectedUserData, counter) {
            handleResult(payload, it, null, attrs, callback)
        }
    }

    override fun getActionByTag(payload: PayloadHolder, id: Id, attrs: AttrForAction): ((ActionCallback) -> Unit)? {
        return when (id) {
            TlvId.CardId -> { callback -> ScanAction().executeMainAction(payload, attrs, callback) }
            else -> null
        }
    }
}