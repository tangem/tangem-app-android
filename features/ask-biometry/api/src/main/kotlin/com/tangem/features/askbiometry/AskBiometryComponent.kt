package com.tangem.features.askbiometry

import com.tangem.core.decompose.factory.ComponentFactory
import com.tangem.core.ui.decompose.ComposableBottomSheetComponent
import com.tangem.core.ui.decompose.ComposableContentComponent

interface AskBiometryComponent : ComposableContentComponent, ComposableBottomSheetComponent {

    data class Params(
        val bottomSheetVariant: Boolean,
        val modelCallbacks: ModelCallbacks,
    )

    interface ModelCallbacks {
        fun onAllowed()
        fun onDenied()
    }

    interface Factory : ComponentFactory<Params, AskBiometryComponent>
}