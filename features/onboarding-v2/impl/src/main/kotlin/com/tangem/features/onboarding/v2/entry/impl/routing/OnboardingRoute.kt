package com.tangem.features.onboarding.v2.entry.impl.routing

import com.tangem.core.decompose.navigation.Route
import com.tangem.domain.models.scan.ScanResponse

sealed class OnboardingRoute : Route {

    data class Wallet12(val scanResponse: ScanResponse) : OnboardingRoute()
}
