package com.tangem.domain.pushnotificationpreferences.models

data class WalletPushNotificationPreferences(
    val transactionAlerts: PushNotificationPreference,
    val offersUpdates: PushNotificationPreference,
    val priceAlerts: PushNotificationPreference,
) {

    fun withCategory(category: PushNotificationCategory, isEnabled: Boolean): WalletPushNotificationPreferences =
        when (category) {
            PushNotificationCategory.TransactionAlerts -> copy(
                transactionAlerts = transactionAlerts.copy(isEnabled = isEnabled),
            )
            PushNotificationCategory.OffersUpdates -> copy(
                offersUpdates = offersUpdates.copy(isEnabled = isEnabled),
            )
            PushNotificationCategory.PriceAlerts -> copy(
                priceAlerts = priceAlerts.copy(isEnabled = isEnabled),
            )
        }
}