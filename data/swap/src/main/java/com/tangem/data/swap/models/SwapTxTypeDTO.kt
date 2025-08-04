package com.tangem.data.swap.models

import com.squareup.moshi.Json
import com.squareup.moshi.JsonClass

@JsonClass(generateAdapter = false)
internal enum class SwapTxTypeDTO {
    @Json(name = "Swap")
    Swap,

    @Json(name = "SendWithSwap")
    SendWithSwap,
}