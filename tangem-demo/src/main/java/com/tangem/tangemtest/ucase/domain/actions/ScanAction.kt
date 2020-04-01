package com.tangem.tangemtest.ucase.domain.actions

import com.tangem.tangemtest._arch.structure.PayloadHolder
import com.tangem.tangemtest.ucase.domain.paramsManager.ActionCallback
import com.tangem.tangemtest.ucase.domain.paramsManager.triggers.afterAction.AfterScanModifier

/**
[REDACTED_AUTHOR]
 */
class ScanAction : BaseAction() {
    override fun executeMainAction(payload: PayloadHolder, attrs: AttrForAction, callback: ActionCallback) {
        attrs.cardManager.scanCard { handleResult(payload, it, AfterScanModifier(), attrs, callback) }
    }
}