package com.tangem.spend.datasource.pay.models.response

import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable

/**
 * Response from `GET v1/customer/cashback/accruals/docs`
 */
@Serializable
data class CashbackAccrualDocsResponse(
    @SerialName("result") val result: Result?,
) {
    @Serializable
    data class Result(
        @SerialName("docs") val docs: List<Doc>?,
    )

    @Serializable
    data class Doc(
        @SerialName("id") val id: String?,
        @SerialName("title") val title: String?,
        @SerialName("url") val url: String?,
    )
}