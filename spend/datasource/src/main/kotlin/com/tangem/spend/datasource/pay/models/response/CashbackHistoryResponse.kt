package com.tangem.spend.datasource.pay.models.response

import com.tangem.domain.models.serialization.SerializedBigDecimal
import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable

/**
 * Response from `GET /v1/customer/cashback/history`
 */
@Serializable
data class CashbackHistoryResponse(
    @SerialName("result") val result: Result?,
) {
    @Serializable
    data class Result(
        @SerialName("items") val items: List<Item>?,
    )

    @Serializable
    data class Item(
        @SerialName("year") val year: Int,
        @SerialName("month") val month: Int,
        @SerialName("confirmed_amount") val confirmedAmount: SerializedBigDecimal,
        @SerialName("currency") val currency: String,
    )
}