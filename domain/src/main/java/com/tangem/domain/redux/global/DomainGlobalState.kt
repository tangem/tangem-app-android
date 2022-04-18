package com.tangem.domain.redux.global

import com.tangem.domain.DomainDialog
import com.tangem.domain.common.ScanResponse

/**
* [REDACTED_AUTHOR]
 */
// [REDACTED_TODO_COMMENT]
data class DomainGlobalState(
    val scanResponse: ScanResponse? = null,
    val dialog: DomainDialog? = null,
)

