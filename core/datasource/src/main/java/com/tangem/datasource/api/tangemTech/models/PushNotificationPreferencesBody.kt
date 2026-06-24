package com.tangem.datasource.api.tangemTech.models

import com.squareup.moshi.Json
import com.squareup.moshi.JsonClass

@JsonClass(generateAdapter = true)
data class PushNotificationPreferencesBody(
    @Json(name = "transactionEventsEnabled")
    val areTransactionEventsEnabled: Boolean,
    @Json(name = "offerUpdatesEnabled")
    val areOfferUpdatesEnabled: Boolean,
    @Json(name = "priceAlertsEnabled")
    val arePriceAlertsEnabled: Boolean,
)