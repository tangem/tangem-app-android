package com.tangem.tangemtest.ucase.domain.paramsManager.managers

import com.tangem.CardManager
import com.tangem.tangemtest._arch.structure.Id
import com.tangem.tangemtest._arch.structure.abstraction.Item
import com.tangem.tangemtest._arch.structure.impl.EditTextItem
import com.tangem.tangemtest.ucase.domain.actions.DepesonalizeAction
import com.tangem.tangemtest.ucase.domain.paramsManager.ActionCallback
import com.tangem.tangemtest.ucase.variants.TlvId

/**
[REDACTED_AUTHOR]
 */
class DepersonalizeItemsManager : BaseItemsManager(DepesonalizeAction()) {
    override fun createItemsList(): List<Item> {
        return listOf(EditTextItem(TlvId.CardId, null))
    }

    override fun invokeMainAction(cardManager: CardManager, callback: ActionCallback) {
        action.executeMainAction(this, getAttrsForAction(cardManager), callback)
    }

    override fun getActionByTag(id: Id, cardManager: CardManager): ((ActionCallback) -> Unit)? {
        return action.getActionByTag(this, id, getAttrsForAction(cardManager))
    }
}