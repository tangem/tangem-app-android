package com.tangem.data.swap.models

import com.squareup.moshi.Json
import com.squareup.moshi.JsonClass

@JsonClass(generateAdapter = true)
data class LastSwappedCryptoCurrencyDTO(
    @Json(name = "userWalletId")
    val userWalletId: String,
    @Json(name = "cryptoCurrencyId")
    val cryptoCurrencyId: String,
)