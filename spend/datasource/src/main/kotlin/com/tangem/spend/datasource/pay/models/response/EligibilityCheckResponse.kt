package com.tangem.spend.datasource.pay.models.response

import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable

/**
 * Response of `bff-v2/v1/eligibility/check`
 */
@Serializable
data class EligibilityCheckResponse(
    @SerialName("result") val result: Result,
) {

    @Serializable
    data class Result(
        @SerialName("available") val isAvailable: Boolean,
        @SerialName("country") val country: String,
        @SerialName("region") val region: String?,
    )
}