package com.tangem.features.pushnotifications.api

import com.tangem.common.routing.AppRoute

data class PushNotificationsParams(
    val isBottomSheet: Boolean = false,
    val nextRoute: AppRoute? = null,
    val modelCallbacks: PushNotificationsModelCallbacks,
    val source: AppRoute.PushNotification.Source,
)