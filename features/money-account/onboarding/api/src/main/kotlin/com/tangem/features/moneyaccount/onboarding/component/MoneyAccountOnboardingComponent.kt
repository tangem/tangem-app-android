package com.tangem.features.moneyaccount.onboarding.component

import com.tangem.core.decompose.factory.ComponentFactory
import com.tangem.core.ui.decompose.ComposableContentComponent

interface MoneyAccountOnboardingComponent : ComposableContentComponent {

    sealed class Params {

        data class Deeplink(val deeplink: String) : Params()

        data object FromMain : Params()

        data object FromDetailsScreen : Params()
    }

    interface Factory : ComponentFactory<Params, MoneyAccountOnboardingComponent>
}