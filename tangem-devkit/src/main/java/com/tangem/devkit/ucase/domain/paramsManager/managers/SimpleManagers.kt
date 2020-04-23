package com.tangem.devkit.ucase.domain.paramsManager.managers

import com.tangem.devkit._arch.structure.impl.EditTextItem
import com.tangem.devkit.ucase.domain.actions.DepersonalizeAction
import com.tangem.devkit.ucase.domain.actions.ScanAction
import com.tangem.devkit.ucase.domain.actions.SignAction
import com.tangem.devkit.ucase.domain.paramsManager.triggers.changeConsequence.SignScanConsequence
import com.tangem.devkit.ucase.variants.TlvId

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