package com.tangem.features.send.v2.api

import com.tangem.core.decompose.factory.ComponentFactory
import com.tangem.core.ui.decompose.ComposableContentComponent
import com.tangem.features.send.v2.api.entity.FeeSelectorUM
import com.tangem.features.send.v2.api.params.FeeSelectorParams

interface FeeSelectorBlockComponent : ComposableContentComponent {

    fun updateState(feeSelectorUM: FeeSelectorUM)

    interface Factory : ComponentFactory<FeeSelectorParams.FeeSelectorBlockParams, FeeSelectorBlockComponent>
}