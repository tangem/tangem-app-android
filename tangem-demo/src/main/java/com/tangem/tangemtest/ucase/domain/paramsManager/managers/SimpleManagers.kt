package com.tangem.tangemtest.ucase.domain.paramsManager.managers

import com.tangem.tangemtest._arch.structure.impl.EditTextItem
import com.tangem.tangemtest.ucase.domain.actions.DepersonalizeAction
import com.tangem.tangemtest.ucase.domain.actions.ScanAction
import com.tangem.tangemtest.ucase.domain.actions.SignAction
import com.tangem.tangemtest.ucase.domain.paramsManager.triggers.changeConsequence.SignScanConsequence
import com.tangem.tangemtest.ucase.variants.TlvId

/**
[REDACTED_AUTHOR]
 */
class ScanItemsManager : BaseItemsManager(ScanAction())

class DepersonalizeItemsManager : BaseItemsManager(DepersonalizeAction()) {

    init {
        setItems(listOf(EditTextItem(TlvId.CardId, null)))
    }
}

class SignItemsManager : BaseItemsManager(SignAction()) {

    init {
        setItemChangeConsequences(SignScanConsequence())
        setItems(listOf(
                EditTextItem(TlvId.CardId, null),
                EditTextItem(TlvId.TransactionOutHash, "Data used for hashing")
        ))
    }
}