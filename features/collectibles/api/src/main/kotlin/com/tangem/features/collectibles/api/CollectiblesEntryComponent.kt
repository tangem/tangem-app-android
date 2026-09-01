package com.tangem.features.collectibles.api

import com.tangem.core.decompose.factory.ComponentFactory
import com.tangem.core.ui.decompose.ComposableContentComponent

interface CollectiblesEntryComponent : ComposableContentComponent {

    interface Factory : ComponentFactory<Unit, CollectiblesEntryComponent>
}