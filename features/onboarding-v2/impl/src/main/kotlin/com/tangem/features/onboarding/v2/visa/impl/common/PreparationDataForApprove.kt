package com.tangem.features.onboarding.v2.visa.impl.common

import com.tangem.domain.visa.model.VisaCustomerWalletDataToSignRequest
import kotlinx.serialization.Serializable

@Serializable
internal data class PreparationDataForApprove(
    val customerWalletAddress: String,
    val request: VisaCustomerWalletDataToSignRequest,
)