package com.tangem.datasource.api.tangemTech.models

import com.squareup.moshi.Json
import com.squareup.moshi.JsonClass

@JsonClass(generateAdapter = true)
data class PushNotificationPreferencesResponse(
    @Json(name = "transactionAlerts") val transactionAlerts: PushNotificationPreferenceState,
    @Json(name = "offersUpdates") val offersUpdates: PushNotificationPreferenceState,
    @Json(name = "priceAlerts") val priceAlerts: PushNotificationPreferenceState,
)