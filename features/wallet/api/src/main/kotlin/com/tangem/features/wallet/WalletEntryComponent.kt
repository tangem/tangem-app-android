package com.tangem.features.wallet

import com.tangem.core.decompose.factory.ComponentFactory
import com.tangem.core.ui.decompose.ComposableContentComponent

interface WalletEntryComponent : ComposableContentComponent {

    interface Factory : ComponentFactory<Unit, WalletEntryComponent>
}