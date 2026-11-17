package com.tangem.features.tangempay.components

import com.tangem.common.routing.AppRoute
import com.tangem.core.decompose.factory.ComponentFactory
import com.tangem.core.ui.decompose.ComposableContentComponent

interface TangemPayOnboardingSuccessComponent : ComposableContentComponent {

    data class Params(val product: AppRoute.TangemPayOnboardingSuccess.Product)

    interface Factory : ComponentFactory<Params, TangemPayOnboardingSuccessComponent>
}