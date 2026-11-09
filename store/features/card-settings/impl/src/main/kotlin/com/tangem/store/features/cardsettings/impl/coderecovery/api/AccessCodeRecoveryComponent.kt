package com.tangem.store.features.cardsettings.impl.coderecovery.api

import com.tangem.core.decompose.factory.ComponentFactory
import com.tangem.core.ui.decompose.ComposableContentComponent

internal interface AccessCodeRecoveryComponent : ComposableContentComponent {

    interface Factory : ComponentFactory<Unit, AccessCodeRecoveryComponent>
}