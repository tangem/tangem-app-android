package com.tangem.features.promobanners.api

import com.tangem.core.decompose.factory.ComponentFactory
import com.tangem.core.ui.decompose.ComposableContentComponent

interface PromoBannersBlockComponent : ComposableContentComponent {

    data class Params(
        val placeholder: Placeholder,
    )

    enum class Placeholder {
        MAIN,
        FEED,
    }

    interface Factory : ComponentFactory<Params, PromoBannersBlockComponent>
}