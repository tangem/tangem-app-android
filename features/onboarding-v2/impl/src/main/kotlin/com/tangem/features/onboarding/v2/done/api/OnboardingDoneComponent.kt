package com.tangem.features.onboarding.v2.done.api

import com.tangem.core.decompose.factory.ComponentFactory
import com.tangem.core.ui.decompose.ComposableContentComponent

interface OnboardingDoneComponent : ComposableContentComponent {

    data class Params(
        val mode: Mode,
        val onDone: () -> Unit,
    )

    enum class Mode {
        WalletCreated, GoodToGo
    }

    interface Factory : ComponentFactory<Params, OnboardingDoneComponent>
}