package com.tangem.features.onboarding.v2.entry.impl.routing

import com.tangem.core.decompose.navigation.Route
import com.tangem.domain.models.scan.ScanResponse
import com.tangem.domain.wallets.models.UserWallet
import com.tangem.features.onboarding.v2.TitleProvider
import com.tangem.features.onboarding.v2.multiwallet.api.OnboardingMultiWalletComponent

sealed class OnboardingRoute : Route {

    data object None : OnboardingRoute()

    data class MultiWallet(
        val titleProvider: TitleProvider,
        val scanResponse: ScanResponse,
        val withSeedPhraseFlow: Boolean,
        val mode: OnboardingMultiWalletComponent.Mode,
        val onDone: (UserWallet) -> Unit,
    ) : OnboardingRoute()

    data class Visa(
        val titleProvider: TitleProvider,
        val scanResponse: ScanResponse,
    ) : OnboardingRoute()

    data class ManageTokens(
        val userWallet: UserWallet,
    ) : OnboardingRoute()

    data class Done(
        val onDone: () -> Unit,
    ) : OnboardingRoute()
}