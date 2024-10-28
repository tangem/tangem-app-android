package com.tangem.features.onboarding.v2.entry

import com.tangem.core.decompose.factory.ComponentFactory
import com.tangem.core.ui.decompose.ComposableContentComponent

interface OnboardingEntryComponent : ComposableContentComponent {

    data class Params(
        val todo: String,
    )

    interface Factory : ComponentFactory<Params, OnboardingEntryComponent>
}
