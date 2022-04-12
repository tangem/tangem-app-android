package com.tangem.domain.redux.global

import com.tangem.domain.DomainStateDialog
import com.tangem.domain.common.ScanResponse
import org.rekotlin.Action

/**
* [REDACTED_AUTHOR]
 */
// [REDACTED_TODO_COMMENT]
sealed class DomainGlobalAction : Action {
    data class SetScanResponse(val scanResponse: ScanResponse?) : DomainGlobalAction()
    data class ShowDialog(val stateDialog: DomainStateDialog?) : DomainGlobalAction()
}