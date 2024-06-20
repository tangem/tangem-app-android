package com.tangem.features.pushnotifications.api.featuretoggles

/**
 * Push notifications feature toggles
 */
interface PushNotificationsFeatureToggles {
    /** Availability of push notifications */
    val isPushNotificationsEnabled: Boolean
}
