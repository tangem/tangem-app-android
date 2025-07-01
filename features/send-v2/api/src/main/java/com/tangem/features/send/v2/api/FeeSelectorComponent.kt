package com.tangem.features.send.v2.api

import com.tangem.core.decompose.factory.ComponentFactory
import com.tangem.core.ui.decompose.ComposableBottomSheetComponent
import com.tangem.features.send.v2.api.params.FeeSelectorParams

interface FeeSelectorComponent : ComposableBottomSheetComponent {
    interface Factory : ComponentFactory<FeeSelectorParams.FeeSelectorDetailsParams, FeeSelectorComponent>
}