package com.tangem.domain.redux.global

import com.tangem.datasource.api.paymentology.PaymentologyApiService
import com.tangem.datasource.api.tangemTech.TangemTechService
import com.tangem.domain.DomainDialog
import com.tangem.domain.common.ScanResponse

/**
 * Created by Anton Zhilenkov on 07/04/2022.
 */
data class DomainGlobalState(
    // there is a part of mirrors from the GlobalState.
    // It updates on GlobalAction.SaveScanNoteResponse -> DomainGlobalAction.SaveScanNoteResponse(scanResponse)
    val scanResponse: ScanResponse? = null,
    //
    val networkServices: NetworkServices = NetworkServices(),
    val dialog: DomainDialog? = null,
)

data class NetworkServices(
    val tangemTechService: TangemTechService = TangemTechService,
    val paymentologyService: PaymentologyApiService = PaymentologyApiService,
)
