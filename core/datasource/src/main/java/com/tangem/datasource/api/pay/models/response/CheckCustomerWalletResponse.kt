package com.tangem.datasource.api.pay.models.response

import com.squareup.moshi.Json
import com.squareup.moshi.JsonClass

@JsonClass(generateAdapter = true)
data class CheckCustomerWalletResponse(
    @Json(name = "result") val result: Result?,
) {
    data class Result(
        @Json(name = "id") val id: String?,
        @Json(name = "is_tangem_pay_enabled") val isTangemPayEnabled: Boolean?,
    )
}