package com.tangem.features.disclaimer.impl.navigation

import com.tangem.features.disclaimer.api.DisclaimerRouter

internal interface InnerDisclaimerRouter : DisclaimerRouter {

    fun openPushNotificationPermission()

    fun openHome()
}