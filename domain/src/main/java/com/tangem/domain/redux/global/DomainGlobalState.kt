package com.tangem.domain.redux.global

import com.tangem.domain.DomainStateDialog
import com.tangem.domain.common.ScanResponse

/**
[REDACTED_AUTHOR]
 */
//TODO: refactoring: is alias for the GlobalState
data class DomainGlobalState(
    val scanResponse: ScanResponse? = null,
    val dialog: DomainStateDialog? = null,
)
