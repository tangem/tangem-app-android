package com.tangem.datasource.api.tangemTech.models

import com.squareup.moshi.Json
import com.squareup.moshi.JsonClass

@JsonClass(generateAdapter = true)
data class NotificationApplicationCreateBody(
    @Json(name = "pushToken") val pushToken: String? = null,
    @Json(name = "platform") val platform: String? = null,
    @Json(name = "device") val device: String? = null,
    @Json(name = "systemVersion") val systemVersion: String? = null,
    @Json(name = "language") val language: String? = null,
    @Json(name = "timezone") val timezone: String? = null,
)