package com.tangem.devkit.ucase.domain.actions

import com.tangem.devkit._arch.structure.Id
import com.tangem.devkit._arch.structure.PayloadHolder
import com.tangem.devkit._arch.structure.abstraction.findItem
import com.tangem.devkit.ucase.domain.paramsManager.ActionCallback
import com.tangem.devkit.ucase.variants.TlvId

class ReadIssuerExtraDataAction : BaseAction() {
    override fun executeMainAction(payload: PayloadHolder, attrs: AttrForAction, callback: ActionCallback) {
        val item = attrs.itemList.findItem(TlvId.CardId) ?: return
        val cardId = item.viewModel.data as? String ?: return

        attrs.tangemSdk.readIssuerExtraData(cardId) { handleResult(payload, it, null, attrs, callback) }
    }

    override fun getActionByTag(payload: PayloadHolder, id: Id, attrs: AttrForAction): ((ActionCallback) -> Unit)? {
        return when (id) {
            TlvId.CardId -> { callback -> ScanAction().executeMainAction(payload, attrs, callback) }
            else -> null
        }
    }
}