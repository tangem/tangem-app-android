package com.tangem.spend.datasource.pay.models.response

import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable

@Serializable
data class CheckCustomerWalletResponse(
    @SerialName("result") val result: Result?,
) {
    @Serializable
    data class Result(
        @SerialName("id") val id: String?,
        @SerialName("is_tangem_pay_enabled") val isTangemPayEnabled: Boolean?,
    )
}