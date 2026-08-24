package com.tangem.features.jointaccount.supportednetworks

import com.tangem.core.decompose.factory.ComponentFactory
import com.tangem.core.ui.decompose.ComposableContentComponent

/**
 * Read-only "Supported networks" modal shown for a joint account: the fixed set of EVM networks a joint
 * account can hold. Presented as a bottom sheet over a scrim; opened via [com.tangem.common.routing.AppRoute].
 */
interface JointSupportedNetworksComponent : ComposableContentComponent {

    interface Factory : ComponentFactory<Unit, JointSupportedNetworksComponent>
}