package com.tangem.domain.redux.global

import com.tangem.domain.DomainDialog
import com.tangem.domain.common.ScanResponse
import org.rekotlin.Action

/**
[REDACTED_AUTHOR]
 */
//TODO: refactoring: is alias for the GlobalAction
sealed class DomainGlobalAction : Action {
    data class SetScanResponse(val scanResponse: ScanResponse?) : DomainGlobalAction()
    data class ShowDialog(val stateDialog: DomainDialog?) : DomainGlobalAction()
}