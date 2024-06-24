package com.tangem.features.disclaimer.impl.navigation

import androidx.fragment.app.Fragment
import com.tangem.common.routing.AppRoute
import com.tangem.common.routing.AppRouter
import com.tangem.features.disclaimer.impl.DisclaimerFragment
import javax.inject.Inject

internal class DefaultDisclaimerRouter @Inject constructor(
    private val appRouter: AppRouter,
) : InnerDisclaimerRouter {

    override fun entryFragment(): Fragment = DisclaimerFragment.create()

    override fun openPushNotificationPermission() {
        appRouter.push(AppRoute.PushNotification)
    }

    override fun openHome() {
        appRouter.push(AppRoute.Home)
    }
}