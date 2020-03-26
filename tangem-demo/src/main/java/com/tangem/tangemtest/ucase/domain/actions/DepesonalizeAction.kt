package com.tangem.tangemtest.ucase.domain.actions

import com.tangem.tangemtest._arch.structure.Id
import com.tangem.tangemtest.ucase.domain.paramsManager.ActionCallback
import com.tangem.tangemtest.ucase.domain.paramsManager.findDataParameter
import com.tangem.tangemtest.ucase.variants.TlvId

/**
[REDACTED_AUTHOR]
 */
class DepesonalizeAction : BaseCardAction() {
    override fun executeMainAction(attrs: AttrForAction, callback: ActionCallback) {
        val item = attrs.paramsList.findDataParameter(TlvId.CardId) ?: return
        val cardId = item.viewModel.data as? String ?: return

        attrs.cardManager.depersonalize(cardId) { handleResponse(it, null, attrs, callback) }
    }

    override fun getActionByTag(id: Id, attrs: AttrForAction): ((ActionCallback) -> Unit)? {
        return when (id) {
            TlvId.CardId -> { callback -> ScanAction().executeMainAction(attrs, callback) }
            else -> null
        }
    }
}