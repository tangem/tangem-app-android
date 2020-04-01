package com.tangem.tangemtest.ucase.domain.actions

import com.tangem.tangemtest._arch.structure.Id
import com.tangem.tangemtest._arch.structure.PayloadHolder
import com.tangem.tangemtest.ucase.domain.paramsManager.ActionCallback
import com.tangem.tangemtest.ucase.domain.paramsManager.findDataItem
import com.tangem.tangemtest.ucase.variants.TlvId

/**
[REDACTED_AUTHOR]
 */
class DepesonalizeAction : BaseAction() {
    override fun executeMainAction(payload: PayloadHolder, attrs: AttrForAction, callback: ActionCallback) {
        val item = attrs.itemList.findDataItem(TlvId.CardId) ?: return
        val cardId = item.viewModel.data as? String ?: return

        attrs.cardManager.depersonalize(cardId) { handleResult(payload, it, null, attrs, callback) }
    }

    override fun getActionByTag(payload: PayloadHolder, id: Id, attrs: AttrForAction): ((ActionCallback) -> Unit)? {
        return when (id) {
            TlvId.CardId -> { callback -> ScanAction().executeMainAction(payload, attrs, callback) }
            else -> null
        }
    }
}