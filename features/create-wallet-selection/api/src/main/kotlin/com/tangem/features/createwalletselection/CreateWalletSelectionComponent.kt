package com.tangem.features.createwalletselection

import com.tangem.core.decompose.factory.ComponentFactory
import com.tangem.core.ui.decompose.ComposableContentComponent

interface CreateWalletSelectionComponent : ComposableContentComponent {
    interface Factory : ComponentFactory<Unit, CreateWalletSelectionComponent>
}