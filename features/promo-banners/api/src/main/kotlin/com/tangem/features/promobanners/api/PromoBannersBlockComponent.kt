package com.tangem.features.promobanners.api

import com.tangem.core.decompose.factory.ComponentFactory
import com.tangem.core.ui.decompose.ComposableContentComponent

interface PromoBannersBlockComponent : ComposableContentComponent {

    fun setVisibleOnScreen(isVisible: Boolean)

    data class Params(
        val placeholder: Placeholder,
        val isInitiallyVisibleOnScreen: Boolean = true,
    )

    enum class Placeholder(val value: String) {
        MAIN("main"),
        FEED("shtorka"),
    }

    interface Factory : ComponentFactory<Params, PromoBannersBlockComponent>
}