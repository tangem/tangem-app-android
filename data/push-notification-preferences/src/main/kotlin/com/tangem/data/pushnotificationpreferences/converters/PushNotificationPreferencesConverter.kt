package com.tangem.data.pushnotificationpreferences.converters

import com.tangem.datasource.api.tangemTech.models.PushNotificationPreferencesResponse
import com.tangem.domain.pushnotificationpreferences.models.PushNotificationPreference
import com.tangem.domain.pushnotificationpreferences.models.WalletPushNotificationPreferences
import com.tangem.utils.converter.Converter

internal object PushNotificationPreferencesConverter :
    Converter<PushNotificationPreferencesResponse, WalletPushNotificationPreferences> {

    override fun convert(value: PushNotificationPreferencesResponse): WalletPushNotificationPreferences =
        WalletPushNotificationPreferences(
            transactionAlerts = PushNotificationPreference(isEnabled = value.transactionEventsEnabled),
            offersUpdates = PushNotificationPreference(isEnabled = value.offerUpdatesEnabled),
            priceAlerts = PushNotificationPreference(isEnabled = value.priceAlertsEnabled),
        )
}