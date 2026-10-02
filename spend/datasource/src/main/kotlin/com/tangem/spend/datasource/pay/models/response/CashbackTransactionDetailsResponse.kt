package com.tangem.spend.datasource.pay.models.response

import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable

/**
 * Response from `GET /v1/customer/cashback/{transaction_id}/details`
 */
@Serializable
data class CashbackTransactionDetailsResponse(
    @SerialName("result") val result: Result?,
) {
    @Serializable
    data class Result(
        @SerialName("cashback") val cashback: TransactionCashbackResponse?,
    )
}