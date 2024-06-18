package com.tangem.feature.referral.router

import com.tangem.common.routing.AppRouter

internal class ReferralRouter(
    private val appRouter: AppRouter,
) {

    fun back() {
        appRouter.pop()
    }
}
