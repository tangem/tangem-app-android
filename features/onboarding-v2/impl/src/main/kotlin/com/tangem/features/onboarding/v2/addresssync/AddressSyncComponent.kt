package com.tangem.features.onboarding.v2.addresssync

import com.tangem.core.decompose.factory.ComponentFactory
import com.tangem.core.ui.decompose.ComposableContentComponent

interface AddressSyncComponent : ComposableContentComponent {

    data class Params(
        val data: Any, // TODO("Will be implemented during [REDACTED_TASK_KEY]")
    )

    interface Factory : ComponentFactory<Params, AddressSyncComponent>
}