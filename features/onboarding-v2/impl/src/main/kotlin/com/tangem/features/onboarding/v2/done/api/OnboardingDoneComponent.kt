package com.tangem.features.onboarding.v2.done.api

import com.tangem.core.decompose.factory.ComponentFactory
import com.tangem.core.ui.decompose.ComposableContentComponent

interface OnboardingDoneComponent : ComposableContentComponent {

    data class Params(val onDone: () -> Unit)

    interface Factory : ComponentFactory<Params, OnboardingDoneComponent>
}