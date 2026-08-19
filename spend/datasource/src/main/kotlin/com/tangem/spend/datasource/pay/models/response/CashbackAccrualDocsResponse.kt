package com.tangem.spend.datasource.pay.models.response

import com.squareup.moshi.Json
import com.squareup.moshi.JsonClass

/**
 * Response from `GET v1/customer/cashback/accruals/docs`
 */
@JsonClass(generateAdapter = true)
data class CashbackAccrualDocsResponse(
    @Json(name = "result") val result: Result?,
) {
    @JsonClass(generateAdapter = true)
    data class Result(
        @Json(name = "docs") val docs: List<Doc>?,
    )

    @JsonClass(generateAdapter = true)
    data class Doc(
        @Json(name = "id") val id: String?,
        @Json(name = "title") val title: String?,
        @Json(name = "url") val url: String?,
    )
}