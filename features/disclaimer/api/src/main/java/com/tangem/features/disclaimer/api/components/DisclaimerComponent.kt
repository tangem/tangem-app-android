package com.tangem.features.disclaimer.api.components

import com.tangem.common.routing.AppRoute
import com.tangem.core.decompose.factory.ComponentFactory
import com.tangem.core.ui.decompose.ComposableContentComponent

interface DisclaimerComponent : ComposableContentComponent {
    interface Factory : ComponentFactory<Params, DisclaimerComponent>

    data class Params(
        val isTosAccepted: Boolean,
        val nextRoute: AppRoute? = null,
    )
}