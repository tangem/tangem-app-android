package com.tangem.tangemtest.ucase.domain.paramsManager.managers

import com.tangem.CardManager
import com.tangem.common.tlv.TlvTag
import com.tangem.tangemtest.ucase.domain.actions.SignAction
import com.tangem.tangemtest.ucase.domain.paramsManager.ActionCallback
import com.tangem.tangemtest.ucase.domain.paramsManager.IncomingParameter

/**
[REDACTED_AUTHOR]
 */
class SignParamsManager : BaseParamsManager(SignAction()) {
    override fun createParamsList(): List<IncomingParameter> {
        return listOf(
                IncomingParameter(TlvTag.CardId, null),
                IncomingParameter(TlvTag.TransactionOutHash, "Data used for hashing")
        )
    }

    override fun invokeMainAction(cardManager: CardManager, callback: ActionCallback) {
        action.executeMainAction(getAttrsForAction(cardManager), callback)
    }

    override fun getActionByTag(tag: TlvTag, cardManager: CardManager): ((ActionCallback) -> Unit)? {
        return action.getActionByTag(tag, getAttrsForAction(cardManager))
    }
}