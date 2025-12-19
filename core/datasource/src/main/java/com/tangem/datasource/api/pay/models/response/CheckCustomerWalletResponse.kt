package com.tangem.datasource.api.pay.models.response

import com.squareup.moshi.Json
import com.squareup.moshi.JsonClass

@JsonClass(generateAdapter = true)
data class CheckCustomerWalletResponse(
    @Json(name = "result") val result: Result?,
    @Json(name = "error") val error: String?,
) {
    data class Result(
        @Json(name = "id") val id: String?,
    )
}