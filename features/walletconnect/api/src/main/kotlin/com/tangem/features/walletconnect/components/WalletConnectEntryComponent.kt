package com.tangem.features.walletconnect.components

import com.tangem.core.decompose.factory.ComponentFactory
import com.tangem.core.ui.decompose.ComposableContentComponent

interface WalletConnectEntryComponent : ComposableContentComponent {
    interface Factory : ComponentFactory<Unit, WalletConnectEntryComponent>
}