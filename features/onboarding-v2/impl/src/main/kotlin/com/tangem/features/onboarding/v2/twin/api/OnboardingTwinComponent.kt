package com.tangem.features.onboarding.v2.twin.api

import com.tangem.core.decompose.factory.ComponentFactory
import com.tangem.core.ui.decompose.ComposableContentComponent
import com.tangem.domain.models.scan.ScanResponse
import com.tangem.features.onboarding.v2.TitleProvider

interface OnboardingTwinComponent : ComposableContentComponent {

    data class Params(
        val titleProvider: TitleProvider,
        val scanResponse: ScanResponse,
        val modelCallbacks: ModelCallbacks,
        val mode: Mode,
    ) {
        enum class Mode {
            CreateWallet, RecreateWallet
        }
    }

    interface ModelCallbacks {
        fun onDone()
    }

    interface Factory : ComponentFactory<Params, OnboardingTwinComponent>
}