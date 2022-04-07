package com.tangem.domain.features.global.redux

import com.tangem.domain.common.ScanResponse

/**
[REDACTED_AUTHOR]
 */
//TODO: refactoring: is alias for the GlobalState
data class DomainGlobalState(
    val scanResponse: ScanResponse? = null,
)
