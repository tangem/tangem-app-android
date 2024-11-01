package com.tangem.features.onboarding.v2.entry.impl.routing

import com.tangem.core.decompose.navigation.Route
import com.tangem.domain.models.scan.ScanResponse
import com.tangem.features.onboarding.v2.TitleProvider

sealed class OnboardingRoute : Route {

    data object None : OnboardingRoute()

    data class Wallet12(
        val titleProvider: TitleProvider,
        val scanResponse: ScanResponse,
        val withSeedPhraseFlow: Boolean,
    ) : OnboardingRoute()
}