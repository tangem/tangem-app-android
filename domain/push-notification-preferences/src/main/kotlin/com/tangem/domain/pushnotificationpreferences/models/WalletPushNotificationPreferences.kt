package com.tangem.domain.pushnotificationpreferences.models

data class WalletPushNotificationPreferences(
    val transactionAlerts: PushNotificationPreference,
    val offersUpdates: PushNotificationPreference,
    val priceAlerts: PushNotificationPreference,
)