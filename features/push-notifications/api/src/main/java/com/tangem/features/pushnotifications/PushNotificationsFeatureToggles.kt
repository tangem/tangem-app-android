package com.tangem.features.pushnotifications

interface PushNotificationsFeatureToggles {

    /** Kill switch for the onboarding "Double Ask" A/B experiment (`twi_1403_onboarding_push_notification_double_ask`). */
    val isOnboardingPushDoubleAskAbEnabled: Boolean

    /**
     * Renders data-only FCM pushes as a system notification client-side. Customer.io sends the alert as an FCM
     * data message (no `notification` block) so the SDK receives `onMessageReceived` in the background and reports
     * `delivered`; the app then draws the notification from `message.data` ([REDACTED_TASK_KEY]).
     */
    val isDataPushAsNotificationEnabled: Boolean
}