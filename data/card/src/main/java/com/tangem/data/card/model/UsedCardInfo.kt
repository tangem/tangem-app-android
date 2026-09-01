package com.tangem.data.card.model

import com.squareup.moshi.Json
import com.squareup.moshi.JsonClass

@JsonClass(generateAdapter = true)
internal data class UsedCardInfo(
    @Json(name = "cardId")
    val cardId: String,
    @Json(name = "isScanned")
    val isScanned: Boolean = false,
    @Json(name = "isActivationStarted")
    val isActivationStarted: Boolean = false,
    @Json(name = "isActivationFinished")
    val isActivationFinished: Boolean = false,
    @Json(name = "hasBackupError")
    val hasBackupError: Boolean = false,
)