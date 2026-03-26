package com.tangem.features.virtualaccount.onboarding.api

import com.tangem.core.decompose.factory.ComponentFactory
import com.tangem.core.ui.decompose.ComposableContentComponent

interface VirtualAccountOnboardingComponent : ComposableContentComponent {

    interface Factory : ComponentFactory<Unit, VirtualAccountOnboardingComponent>
}