package com.tangem.data.wallets.balance

import com.squareup.moshi.Json
import com.squareup.moshi.JsonClass

@JsonClass(generateAdapter = false)
internal enum class WalletBalanceStateDTO {
    @Json(name = "ToppedUp") ToppedUp,

    @Json(name = "Empty") Empty,

    @Json(name = "Error") Error,
}