package com.tangem.features.managetokens.component

import com.tangem.core.decompose.factory.ComponentFactory
import com.tangem.core.ui.decompose.ComposableContentComponent

interface ManageTokensComponent : ComposableContentComponent {

    data class Params(
        val mode: ManageTokensMode,
        val source: ManageTokensSource,
    )

    interface Factory : ComponentFactory<Params, ManageTokensComponent>
}