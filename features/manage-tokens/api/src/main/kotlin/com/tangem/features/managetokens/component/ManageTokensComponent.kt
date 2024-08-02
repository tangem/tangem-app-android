package com.tangem.features.managetokens.component

import com.tangem.core.decompose.factory.ComponentFactory
import com.tangem.core.ui.decompose.ComposableContentComponent

interface ManageTokensComponent : ComposableContentComponent {

    class Params

    interface Factory : ComponentFactory<Params, ManageTokensComponent>
}