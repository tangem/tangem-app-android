package com.tangem.features.forceupdate

import com.tangem.core.decompose.factory.ComponentFactory
import com.tangem.core.ui.decompose.ComposableContentComponent

interface ForceUpdateComponent : ComposableContentComponent {

    interface Factory : ComponentFactory<Params, ForceUpdateComponent>

    data class Params(val mode: Mode)

    enum class Mode { Force, Brick, OsTooOld, Optional }
}