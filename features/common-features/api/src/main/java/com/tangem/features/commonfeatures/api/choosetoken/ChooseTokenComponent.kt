package com.tangem.features.commonfeatures.api.choosetoken

import com.tangem.core.decompose.factory.ComponentFactory
import com.tangem.core.ui.decompose.ComposableContentComponent

interface ChooseTokenComponent : ComposableContentComponent {

    data class Params(
        val bridge: ChooseTokenBridge,
    )

    interface Factory : ComponentFactory<Params, ChooseTokenComponent>
}