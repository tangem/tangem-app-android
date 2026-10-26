package com.tangem.features.jointaccount.supportednetworks

import com.tangem.core.decompose.factory.ComponentFactory
import com.tangem.core.ui.decompose.ComposableBottomSheetComponent

interface JointSupportedNetworksComponent : ComposableBottomSheetComponent {
    interface Factory : ComponentFactory<Params, JointSupportedNetworksComponent>

    data class Params(val onDismiss: () -> Unit)
}