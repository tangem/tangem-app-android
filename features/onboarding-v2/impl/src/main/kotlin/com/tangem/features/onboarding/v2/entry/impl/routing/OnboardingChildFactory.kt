package com.tangem.features.onboarding.v2.entry.impl.routing

import com.tangem.core.decompose.context.AppComponentContext
import com.tangem.features.onboarding.v2.wallet12.api.OnboardingWallet12Component
import javax.inject.Inject

internal class OnboardingChildFactory @Inject constructor(
    private val wallet12ComponentFactory: OnboardingWallet12Component.Factory,
) {

    fun createChild(route: OnboardingRoute, childContext: AppComponentContext) {
        when (route) {
            is OnboardingRoute.Wallet12 -> wallet12ComponentFactory.create(
                context = childContext,
                params = OnboardingWallet12Component.Params(
                    scanResponse = route.scanResponse,
                ),
            )
        }
    }
}
