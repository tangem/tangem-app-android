package com.tangem.domain.features.global.redux

import com.tangem.domain.common.ScanResponse
import org.rekotlin.Action

/**
* [REDACTED_AUTHOR]
 */
// [REDACTED_TODO_COMMENT]
sealed class DomainGlobalAction : Action {
    data class SetScanResponse(val scanResponse: ScanResponse?) : DomainGlobalAction()
}