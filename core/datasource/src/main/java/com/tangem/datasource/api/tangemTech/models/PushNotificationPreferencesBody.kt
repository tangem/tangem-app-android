package com.tangem.datasource.api.tangemTech.models

import com.squareup.moshi.Json
import com.squareup.moshi.JsonClass

@JsonClass(generateAdapter = true)
data class PushNotificationPreferencesBody(
    @Json(name = "transactionAlerts")
    val areTransactionAlertsEnabled: Boolean,
    @Json(name = "offersUpdates")
    val areOffersUpdatesEnabled: Boolean,
    @Json(name = "priceAlerts")
    val arePriceAlertsEnabled: Boolean,
)