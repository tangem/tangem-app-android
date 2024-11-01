package com.tangem.features.onboarding.v2.entry.impl.routing

import com.tangem.core.decompose.context.AppComponentContext
import com.tangem.features.onboarding.v2.multiwallet.api.OnboardingMultiWalletComponent
import javax.inject.Inject

internal class OnboardingChildFactory @Inject constructor(
    private val wallet12ComponentFactory: OnboardingMultiWalletComponent.Factory,
) {

    fun createChild(route: OnboardingRoute, childContext: AppComponentContext): Any {
        return when (route) {
            is OnboardingRoute.Wallet12 -> wallet12ComponentFactory.create(
                context = childContext,
                params = OnboardingMultiWalletComponent.Params(
                    scanResponse = route.scanResponse,
                    withSeedPhraseFlow = route.withSeedPhraseFlow,
                    titleProvider = route.titleProvider,
                ),
            )
            else -> Unit
        }
    }
}