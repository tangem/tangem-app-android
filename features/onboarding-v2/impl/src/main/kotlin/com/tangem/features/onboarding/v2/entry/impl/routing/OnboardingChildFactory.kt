package com.tangem.features.onboarding.v2.entry.impl.routing

import com.tangem.core.decompose.context.AppComponentContext
import com.tangem.features.managetokens.component.OnboardingManageTokensComponent
import com.tangem.features.onboarding.v2.done.api.OnboardingDoneComponent
import com.tangem.features.onboarding.v2.multiwallet.api.OnboardingMultiWalletComponent
import javax.inject.Inject

internal class OnboardingChildFactory @Inject constructor(
    private val multiWalletComponentFactory: OnboardingMultiWalletComponent.Factory,
    private val onboardingManageTokensComponentFactory: OnboardingManageTokensComponent.Factory,
    private val onboardingDoneComponentFactory: OnboardingDoneComponent.Factory,
) {

    fun createChild(route: OnboardingRoute, childContext: AppComponentContext, onManageTokensDone: () -> Unit): Any {
        return when (route) {
            is OnboardingRoute.MultiWallet -> multiWalletComponentFactory.create(
                context = childContext,
                params = OnboardingMultiWalletComponent.Params(
                    scanResponse = route.scanResponse,
                    withSeedPhraseFlow = route.withSeedPhraseFlow,
                    titleProvider = route.titleProvider,
                    onDone = route.onDone,
                    mode = route.mode,
                ),
            )
            is OnboardingRoute.ManageTokens -> onboardingManageTokensComponentFactory.create(
                context = childContext,
                params = OnboardingManageTokensComponent.Params(
                    userWalletId = route.userWallet.walletId,
                ),
                onDone = onManageTokensDone,
            )
            is OnboardingRoute.Done -> onboardingDoneComponentFactory.create(
                context = childContext,
                params = OnboardingDoneComponent.Params(
                    onDone = route.onDone,
                ),
            )
            else -> Unit
        }
    }
}