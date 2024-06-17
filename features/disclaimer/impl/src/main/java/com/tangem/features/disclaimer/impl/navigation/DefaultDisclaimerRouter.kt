package com.tangem.features.disclaimer.impl.navigation

import androidx.fragment.app.Fragment
import com.tangem.common.routing.AppRoute
import com.tangem.common.routing.AppRouter
import com.tangem.features.disclaimer.impl.DisclaimerFragment

internal class DefaultDisclaimerRouter(
    private val appRouter: AppRouter,
) : InnerDisclaimerRouter {

    override fun entryFragment(): Fragment = DisclaimerFragment.create()

    override fun openPushNotificationPermission() {
// [REDACTED_TODO_COMMENT]
    }

    override fun openHome() {
        appRouter.push(AppRoute.Home)
    }
}
