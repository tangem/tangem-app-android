package com.tangem.datasource.local.card

import com.squareup.moshi.Json
import com.squareup.moshi.JsonClass

@JsonClass(generateAdapter = true)
data class UsedCardInfo(
    @Json(name = "cardId")
    val cardId: String,
    @Json(name = "isScanned")
    val isScanned: Boolean = false,
    @Json(name = "isActivationStarted")
    val isActivationStarted: Boolean = false,
    @Json(name = "isActivationFinished")
    val isActivationFinished: Boolean = false,
)