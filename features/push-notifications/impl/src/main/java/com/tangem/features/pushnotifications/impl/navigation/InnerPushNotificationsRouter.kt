package com.tangem.features.pushnotifications.impl.navigation

import com.tangem.features.pushnotifications.api.navigation.PushNotificationsRouter

interface InnerPushNotificationsRouter : PushNotificationsRouter {

    fun openHome()
}