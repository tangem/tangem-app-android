package com.tangem.domain.redux.global

import com.tangem.domain.models.scan.ScanResponse
import org.rekotlin.Action

/**
* [REDACTED_AUTHOR]
 */
// [REDACTED_TODO_COMMENT]
sealed class DomainGlobalAction : Action {
    data class SaveScanNoteResponse(val scanResponse: ScanResponse) : DomainGlobalAction()
}
