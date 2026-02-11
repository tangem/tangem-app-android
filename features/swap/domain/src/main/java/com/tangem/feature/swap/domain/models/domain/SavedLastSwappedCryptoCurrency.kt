package com.tangem.feature.swap.domain.models.domain

import com.squareup.moshi.Json
import com.squareup.moshi.JsonClass

@JsonClass(generateAdapter = true)
data class SavedLastSwappedCryptoCurrency(
    @Json(name = "userWalletId")
    val userWalletId: String,
    @Json(name = "cryptoCurrencyId")
    val cryptoCurrencyId: String,
)