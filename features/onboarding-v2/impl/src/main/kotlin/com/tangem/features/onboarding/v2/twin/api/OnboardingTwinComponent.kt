package com.tangem.features.onboarding.v2.twin.api

import com.tangem.core.decompose.factory.ComponentFactory
import com.tangem.core.ui.decompose.ComposableContentComponent
import com.tangem.domain.models.scan.ScanResponse
import com.tangem.features.onboarding.v2.TitleProvider

interface OnboardingTwinComponent : ComposableContentComponent {

    data class Params(
        val scanResponse: ScanResponse,
        val modelCallbacks: ModelCallbacks,
        val titleProvider: TitleProvider,
        val mode: Mode,
    ) {
        enum class Mode {
            WelcomeOnly, CreateWallet, RecreateWallet
        }
    }

    interface ModelCallbacks {
        fun onDone()
        fun onBack()
    }

    interface Factory : ComponentFactory<Params, OnboardingTwinComponent>
}