package com.tangem.tap.features.details.ui.cardsettings.coderecovery.api

import com.tangem.core.decompose.factory.ComponentFactory
import com.tangem.core.ui.decompose.ComposableContentComponent

interface AccessCodeRecoveryComponent : ComposableContentComponent {

    interface Factory : ComponentFactory<Unit, AccessCodeRecoveryComponent>
}