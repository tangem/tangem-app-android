package com.tangem.features.pushnotifications

interface PushNotificationsFeatureToggles {

    /** Kill switch for the onboarding "Double Ask" A/B experiment (`twi_1403_onboarding_push_notification_double_ask`). */
    val isOnboardingPushDoubleAskAbEnabled: Boolean
}