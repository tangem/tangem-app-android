package com.tangem.domain.analytics.model

import com.squareup.moshi.Json
import com.squareup.moshi.JsonClass

@JsonClass(generateAdapter = false)
enum class WalletBalanceState {
    @Json(name = "ToppedUp") ToppedUp,

    @Json(name = "Empty") Empty,

    @Json(name = "Error") Error,
}