package com.tangem.features.onboarding.usedcard

import com.tangem.core.decompose.factory.ComponentFactory
import com.tangem.core.ui.decompose.ComposableContentComponent
import com.tangem.domain.models.scan.ScanResponse

interface UsedCardOnboardingComponent : ComposableContentComponent {

    data class Params(
        val scanResponse: ScanResponse,
    )

    interface Factory : ComponentFactory<Params, UsedCardOnboardingComponent>
}