package com.tangem.tangemtest.ucase.domain.actions

import com.tangem.tangemtest._arch.structure.Id
import com.tangem.tangemtest._arch.structure.PayloadHolder
import com.tangem.tangemtest.ucase.domain.paramsManager.ActionCallback
import com.tangem.tangemtest.ucase.domain.paramsManager.findDataItem
import com.tangem.tangemtest.ucase.variants.TlvId
import ru.dev.gbixahue.eu4d.lib.kotlin.stringOf

/**
* [REDACTED_AUTHOR]
 */
class SignAction : BaseAction() {
    override fun executeMainAction(payload: PayloadHolder, attrs: AttrForAction, callback: ActionCallback) {
        val dataForHashing = attrs.itemList.findDataItem(TlvId.TransactionOutHash) ?: return
        val hash = dataForHashing.getData() as? ByteArray ?: return
        val cardId = attrs.itemList.findDataItem(TlvId.CardId)?.viewModel?.data ?: return

        attrs.cardManager.sign(arrayOf(hash), stringOf(cardId)) { handleResult(payload, it, null, attrs, callback) }
    }

    override fun getActionByTag(payload: PayloadHolder, id: Id, attrs: AttrForAction): ((ActionCallback) -> Unit)? {
        return when (id) {
            TlvId.CardId -> { callback -> ScanAction().executeMainAction(payload, attrs, callback) }
            else -> null
        }
    }
}