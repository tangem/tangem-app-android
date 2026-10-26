package com.tangem.spend.datasource.pay.models.response

import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable

/** Response of the single-transaction endpoint (`GET v1/transactions/{transaction_id}`). */
@Serializable
data class TangemPayTransactionResponse(
    @SerialName("result") val result: TangemPayTxHistoryResponse.Transaction,
)