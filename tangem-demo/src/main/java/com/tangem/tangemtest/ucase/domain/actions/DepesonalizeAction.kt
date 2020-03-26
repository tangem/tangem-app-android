package com.tangem.tangemtest.ucase.domain.actions

import com.tangem.common.tlv.TlvTag
import com.tangem.tangemtest.ucase.domain.paramsManager.ActionCallback
import com.tangem.tangemtest.ucase.domain.paramsManager.findParameter

/**
[REDACTED_AUTHOR]
 */
class DepesonalizeAction : BaseCardAction() {
    override fun executeMainAction(attrs: AttrForAction, callback: ActionCallback) {
        val cardId = attrs.paramsList.findParameter(TlvTag.CardId)?.data as? String ?: return

        attrs.cardManager.depersonalize(cardId) { handleResponse(it, null, attrs, callback) }
    }

    override fun getActionByTag(tag: TlvTag, attrs: AttrForAction): ((ActionCallback) -> Unit)? {
        return when (tag) {
            TlvTag.CardId -> { callback -> ScanAction().executeMainAction(attrs, callback) }
            else -> null
        }
    }
}