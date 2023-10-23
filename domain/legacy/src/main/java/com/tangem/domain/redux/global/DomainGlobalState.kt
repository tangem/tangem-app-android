package com.tangem.domain.redux.global

import com.tangem.datasource.api.paymentology.PaymentologyApiService
import com.tangem.datasource.api.tangemTech.TangemTechService
import com.tangem.domain.models.scan.ScanResponse

/**
[REDACTED_AUTHOR]
 */
data class DomainGlobalState(
    // there is a part of mirrors from the GlobalState.
    // It updates on GlobalAction.SaveScanNoteResponse -> DomainGlobalAction.SaveScanNoteResponse(scanResponse)
    val scanResponse: ScanResponse? = null,
    //
    val networkServices: NetworkServices = NetworkServices(),
)

data class NetworkServices(
    val tangemTechService: TangemTechService = TangemTechService,
    val paymentologyService: PaymentologyApiService = PaymentologyApiService,
)