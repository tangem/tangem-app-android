package com.tangem.domain.redux.global

import com.tangem.domain.models.scan.ScanResponse
import org.rekotlin.Action

/**
[REDACTED_AUTHOR]
 */
// TODO: refactoring: is alias for the GlobalAction
sealed class DomainGlobalAction : Action {
    data class SaveScanNoteResponse(val scanResponse: ScanResponse) : DomainGlobalAction()
}