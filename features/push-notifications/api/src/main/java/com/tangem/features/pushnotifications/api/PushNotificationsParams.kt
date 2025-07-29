package com.tangem.features.pushnotifications.api

data class PushNotificationsParams(
    val isBottomSheet: Boolean = false,
    val modelCallbacks: PushNotificationsModelCallbacks,
)