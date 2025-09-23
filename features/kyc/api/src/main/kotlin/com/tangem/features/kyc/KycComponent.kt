package com.tangem.features.kyc

import com.tangem.core.decompose.factory.ComponentFactory
import com.tangem.core.ui.decompose.ComposableContentComponent

interface KycComponent : ComposableContentComponent {

    data object Params

    interface Factory : ComponentFactory<Params, KycComponent>
}