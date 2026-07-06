package com.tangem.features.foryou

import com.tangem.core.decompose.factory.ComponentFactory
import com.tangem.core.ui.decompose.ComposableModularBottomSheetContentComponent

interface ForYouComponent : ComposableModularBottomSheetContentComponent {

    interface Factory : ComponentFactory<Unit, ForYouComponent>
}