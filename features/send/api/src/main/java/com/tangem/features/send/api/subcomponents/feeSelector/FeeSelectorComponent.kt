package com.tangem.features.send.api.subcomponents.feeSelector

import com.tangem.core.decompose.context.AppComponentContext
import com.tangem.core.ui.decompose.ComposableBottomSheetComponent
import com.tangem.features.send.api.subcomponents.feeSelector.params.FeeSelectorParams

interface FeeSelectorComponent : ComposableBottomSheetComponent {
    interface Factory {
        fun create(
            context: AppComponentContext,
            params: FeeSelectorParams.FeeSelectorDetailsParams,
            onDismiss: () -> Unit,
        ): FeeSelectorComponent
    }
}