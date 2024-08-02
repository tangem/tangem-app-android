package com.tangem.features.pushnotifications.impl.navigation

import androidx.fragment.app.Fragment
import com.tangem.common.routing.AppRoute
import com.tangem.common.routing.AppRouter
import com.tangem.features.pushnotifications.impl.PushNotificationsFragment
import javax.inject.Inject

internal class DefaultPushNotificationsRouter @Inject constructor(
    private val appRouter: AppRouter,
) : InnerPushNotificationsRouter {

    override fun entryFragment(): Fragment = PushNotificationsFragment.create()

    override fun openHome() {
        appRouter.push(AppRoute.Home)
    }
}
