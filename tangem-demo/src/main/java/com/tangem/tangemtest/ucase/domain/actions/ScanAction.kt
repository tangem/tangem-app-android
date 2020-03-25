package com.tangem.tangemtest.ucase.domain.actions

import com.tangem.tangemtest.ucase.domain.paramsManager.ActionCallback
import com.tangem.tangemtest.ucase.domain.paramsManager.triggers.afterAction.AfterScanModifier

class ScanAction : BaseCardAction() {
    override fun executeMainAction(attrs: AttrForAction, callback: ActionCallback) {
        attrs.cardManager.scanCard { handleResponse(it, AfterScanModifier(), attrs, callback) }
    }
}