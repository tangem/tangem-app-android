package com.tangem.features.details.component

import com.tangem.core.decompose.context.AppComponentContext
import com.tangem.core.ui.decompose.ComposableContentComponent
import com.tangem.features.introduction.AddWalletComponent

interface UserWalletListComponent : ComposableContentComponent {

    /** Owned by the block's model, so the retained Add wallet sheet may hold them */
    val addWalletCallbacks: AddWalletComponent.ModelCallbacks

    data class Params(val onAddWalletSheetRequest: () -> Unit)

    interface Factory {
        fun create(context: AppComponentContext, params: Params): UserWalletListComponent
    }
}