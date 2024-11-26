package com.tangem.features.onboarding.v2.entry.impl.routing

import com.tangem.core.decompose.context.AppComponentContext
import com.tangem.features.managetokens.component.ManageTokensComponent
import com.tangem.features.managetokens.component.ManageTokensSource
import com.tangem.features.onboarding.v2.multiwallet.api.OnboardingMultiWalletComponent
import javax.inject.Inject

internal class OnboardingChildFactory @Inject constructor(
    private val multiWalletComponentFactory: OnboardingMultiWalletComponent.Factory,
    private val manageTokensComponentFactory: ManageTokensComponent.Factory,
) {

    fun createChild(route: OnboardingRoute, childContext: AppComponentContext): Any {
        return when (route) {
            is OnboardingRoute.MultiWallet -> multiWalletComponentFactory.create(
                context = childContext,
                params = OnboardingMultiWalletComponent.Params(
                    scanResponse = route.scanResponse,
                    withSeedPhraseFlow = route.withSeedPhraseFlow,
                    titleProvider = route.titleProvider,
                    onDone = route.onDone,
                ),
            )
            is OnboardingRoute.ManageTokens -> manageTokensComponentFactory.create( // TODO replace with wrapper
                context = childContext,
                params = ManageTokensComponent.Params(
                    userWalletId = route.userWallet.walletId,
                    source = ManageTokensSource.ONBOARDING,
                ),
            )
            else -> Unit
        }
    }
}
