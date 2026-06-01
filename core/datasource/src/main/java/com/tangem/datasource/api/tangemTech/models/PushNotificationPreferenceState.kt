package com.tangem.datasource.api.tangemTech.models

import com.squareup.moshi.Json
import com.squareup.moshi.JsonClass

@JsonClass(generateAdapter = true)
data class PushNotificationPreferenceState(
    @Json(name = "isEnabled") val isEnabled: Boolean,
    @Json(name = "isVisible") val isVisible: Boolean,
)