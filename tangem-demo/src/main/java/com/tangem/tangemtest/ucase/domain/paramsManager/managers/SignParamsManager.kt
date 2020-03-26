package com.tangem.tangemtest.ucase.domain.paramsManager.managers

import com.tangem.CardManager
import com.tangem.tangemtest._arch.structure.Id
import com.tangem.tangemtest._arch.structure.abstraction.Item
import com.tangem.tangemtest._arch.structure.impl.EditTextItem
import com.tangem.tangemtest.ucase.domain.actions.SignAction
import com.tangem.tangemtest.ucase.domain.paramsManager.ActionCallback
import com.tangem.tangemtest.ucase.variants.TlvId

/**
[REDACTED_AUTHOR]
 */
class SignParamsManager : BaseParamsManager(SignAction()) {
    override fun createParamsList(): List<Item> {
        return listOf(
                EditTextItem(TlvId.CardId, null),
                EditTextItem(TlvId.TransactionOutHash, "Data used for hashing")
        )
    }

    override fun invokeMainAction(cardManager: CardManager, callback: ActionCallback) {
        action.executeMainAction(getAttrsForAction(cardManager), callback)
    }

    override fun getActionByTag(id: Id, cardManager: CardManager): ((ActionCallback) -> Unit)? {
        return action.getActionByTag(id, getAttrsForAction(cardManager))
    }
}