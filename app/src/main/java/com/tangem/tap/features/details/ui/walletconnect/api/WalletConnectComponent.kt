package com.tangem.tap.features.details.ui.walletconnect.api

import com.tangem.core.decompose.factory.ComponentFactory
import com.tangem.core.ui.decompose.ComposableContentComponent

interface WalletConnectComponent : ComposableContentComponent {
    interface Factory : ComponentFactory<Unit, WalletConnectComponent>
}