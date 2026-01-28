package com.tangem.datasource.api.pay.models.response

import com.squareup.moshi.Json

data class CardBalanceResponse(
    @Json(name = "result") val result: BalanceResponse?,
)