package com.tangem.features.wallet

import com.tangem.core.decompose.factory.ComponentFactory
import com.tangem.core.ui.decompose.ComposableContentComponent

/** Sheet that links a wallet to this device, shown over any screen. Created once by the root component. */
interface WalletLinkComponent : ComposableContentComponent {

    interface Factory : ComponentFactory<Unit, WalletLinkComponent>
}