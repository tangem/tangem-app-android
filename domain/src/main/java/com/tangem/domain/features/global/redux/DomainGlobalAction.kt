package com.tangem.domain.features.global.redux

import com.tangem.domain.common.ScanResponse
import org.rekotlin.Action

/**
[REDACTED_AUTHOR]
 */
//TODO: refactoring: is alias for the GlobalAction
sealed class DomainGlobalAction : Action {
    data class SetScanResponse(val scanResponse: ScanResponse?) : DomainGlobalAction()
}