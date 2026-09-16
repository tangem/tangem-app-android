package com.tangem.features.jointaccount.supportednetworks

import com.tangem.core.decompose.factory.ComponentFactory
import com.tangem.core.ui.decompose.ComposableBottomSheetComponent

/**
 * Read-only "Supported networks" modal shown for a joint account: the fixed set of EVM networks a joint
 * account can hold. Presented as a bottom-sheet child slot hosted by the screen that opens it (e.g. Account
 * Details) — not a routed screen.
 */
interface JointSupportedNetworksComponent : ComposableBottomSheetComponent {

    interface Factory : ComponentFactory<Params, JointSupportedNetworksComponent>

    /** @property onDismiss closes the hosting slot. */
    data class Params(val onDismiss: () -> Unit)
}