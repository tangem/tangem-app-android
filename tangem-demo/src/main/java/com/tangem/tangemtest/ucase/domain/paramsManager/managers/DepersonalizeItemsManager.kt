package com.tangem.tangemtest.ucase.domain.paramsManager.managers

import com.tangem.TangemSdk
import com.tangem.tangemtest._arch.structure.Id
import com.tangem.tangemtest._arch.structure.abstraction.Item
import com.tangem.tangemtest._arch.structure.impl.EditTextItem
import com.tangem.tangemtest.ucase.domain.actions.DepersonalizeAction
import com.tangem.tangemtest.ucase.domain.paramsManager.ActionCallback
import com.tangem.tangemtest.ucase.variants.TlvId

/**
[REDACTED_AUTHOR]
 */
class DepersonalizeItemsManager : BaseItemsManager(DepersonalizeAction()) {
    override fun createItemsList(): List<Item> {
        return listOf(EditTextItem(TlvId.CardId, null))
    }

    override fun invokeMainAction(tangemSdk: TangemSdk, callback: ActionCallback) {
        action.executeMainAction(this, getAttrsForAction(tangemSdk), callback)
    }

    override fun getActionByTag(id: Id, tangemSdk: TangemSdk): ((ActionCallback) -> Unit)? {
        return action.getActionByTag(this, id, getAttrsForAction(tangemSdk))
    }
}