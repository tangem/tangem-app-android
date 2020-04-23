package com.tangem.devkit.ucase.domain.actions

import com.tangem.devkit._arch.structure.PayloadHolder
import com.tangem.devkit.ucase.domain.paramsManager.ActionCallback
import com.tangem.devkit.ucase.domain.paramsManager.triggers.afterAction.AfterScanModifier

/**
[REDACTED_AUTHOR]
 */
class ScanAction : BaseAction() {
    override fun executeMainAction(payload: PayloadHolder, attrs: AttrForAction, callback: ActionCallback) {
        attrs.tangemSdk.scanCard { handleResult(payload, it, AfterScanModifier(), attrs, callback) }
    }
}