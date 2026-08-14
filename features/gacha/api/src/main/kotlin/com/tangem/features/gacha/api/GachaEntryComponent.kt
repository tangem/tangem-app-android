package com.tangem.features.gacha.api

import com.tangem.core.decompose.factory.ComponentFactory
import com.tangem.core.ui.decompose.ComposableContentComponent

interface GachaEntryComponent : ComposableContentComponent {

    interface Factory : ComponentFactory<Unit, GachaEntryComponent>
}