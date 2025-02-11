package com.tangem.features.onboarding.v2.visa.impl.common

import com.tangem.domain.models.scan.ScanResponse
import com.tangem.domain.visa.model.VisaCustomerWalletDataToSignRequest

internal data class ActivationReadyEvent(
    val customerWalletDataToSignRequest: VisaCustomerWalletDataToSignRequest,
    val customerWalletTargetAddress: String,
    val newScanResponse: ScanResponse,
)