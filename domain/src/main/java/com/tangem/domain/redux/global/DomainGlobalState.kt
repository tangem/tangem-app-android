package com.tangem.domain.redux.global

import com.tangem.domain.DomainDialog
import com.tangem.domain.common.LogConfig
import com.tangem.domain.common.NetworkLogConfig
import com.tangem.domain.common.ScanResponse
import com.tangem.network.api.tangemTech.TangemTechService

/**
[REDACTED_AUTHOR]
 */
data class DomainGlobalState(
    // there is a part of mirrors from the GlobalState.
    // It updates on GlobalAction.SaveScanNoteResponse -> DomainGlobalAction.SaveScanNoteResponse(scanResponse)
    val scanResponse: ScanResponse? = null,
    //
    val logConfig: LogConfig = LogConfig.buildBased(),
    val networkServices: NetworkServices = NetworkServices(logConfig.network),
    val dialog: DomainDialog? = null,
)

data class NetworkServices(
    val networkLogConfig: NetworkLogConfig,
    val tangemTechService: TangemTechService = TangemTechService(networkLogConfig.tangemTechService)
)
