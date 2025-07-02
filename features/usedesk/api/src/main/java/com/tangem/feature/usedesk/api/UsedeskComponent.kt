package com.tangem.feature.usedesk.api

import com.tangem.core.decompose.factory.ComponentFactory
import com.tangem.core.ui.decompose.ComposableContentComponent

interface UsedeskComponent : ComposableContentComponent {

    data class Params(
        val feedback: String? = null, // TODO AND-11146
    )

    interface Factory : ComponentFactory<Params, UsedeskComponent>
}
