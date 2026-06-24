package com.tangem.features.send.api.subcomponents.feeSelector

import com.tangem.core.decompose.context.AppComponentContext
import com.tangem.core.ui.decompose.ComposableContentComponent
import com.tangem.features.send.api.subcomponents.feeSelector.entity.FeeSelectorUM
import com.tangem.features.send.api.subcomponents.feeSelector.params.FeeSelectorParams

interface FeeSelectorBlockComponent : ComposableContentComponent {

    fun updateState(feeSelectorUM: FeeSelectorUM)

    interface Factory {
        fun create(
            context: AppComponentContext,
            params: FeeSelectorParams.FeeSelectorBlockParams,
            onResult: (feeSelectorUM: FeeSelectorUM) -> Unit,
        ): FeeSelectorBlockComponent
    }
}