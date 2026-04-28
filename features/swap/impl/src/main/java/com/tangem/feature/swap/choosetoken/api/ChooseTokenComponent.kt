package com.tangem.feature.swap.choosetoken.api

import com.tangem.core.decompose.factory.ComponentFactory
import com.tangem.core.ui.decompose.ComposableContentComponent

internal interface ChooseTokenComponent : ComposableContentComponent {

    data class Params(
        val bridge: ChooseTokenBridge,
    )

    interface Factory : ComponentFactory<Params, ChooseTokenComponent>
}