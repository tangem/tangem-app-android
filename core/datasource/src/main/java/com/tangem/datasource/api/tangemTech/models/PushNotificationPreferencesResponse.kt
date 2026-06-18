package com.tangem.datasource.api.tangemTech.models

import com.squareup.moshi.Json
import com.squareup.moshi.JsonClass

@JsonClass(generateAdapter = true)
data class PushNotificationPreferencesResponse(
    @Json(name = "transactionEventsEnabled") val transactionEventsEnabled: Boolean,
    @Json(name = "offerUpdatesEnabled") val offerUpdatesEnabled: Boolean,
    @Json(name = "priceAlertsEnabled") val priceAlertsEnabled: Boolean,
)