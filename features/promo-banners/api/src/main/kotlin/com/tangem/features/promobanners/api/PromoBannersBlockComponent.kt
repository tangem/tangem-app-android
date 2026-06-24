package com.tangem.features.promobanners.api

import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.Dp
import com.tangem.core.decompose.factory.ComponentFactory

interface PromoBannersBlockComponent {

    @Composable
    fun ContentWithPadding(horizontalItemPadding: Dp, modifier: Modifier)

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