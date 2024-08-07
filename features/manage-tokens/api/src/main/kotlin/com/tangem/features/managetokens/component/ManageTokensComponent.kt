package com.tangem.features.managetokens.component

import com.tangem.core.decompose.factory.ComponentFactory
import com.tangem.core.ui.decompose.ComposableContentComponent

interface ManageTokensComponent : ComposableContentComponent {

    data class Params(val mode: Mode)

    enum class Mode { READ_ONLY, MANAGE, }
    interface Factory : ComponentFactory<Params, ManageTokensComponent>
}
