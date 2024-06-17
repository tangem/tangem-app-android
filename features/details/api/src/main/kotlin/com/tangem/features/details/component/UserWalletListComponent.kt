package com.tangem.features.details.component

import com.tangem.core.decompose.context.AppComponentContext
import com.tangem.core.ui.ComposableContentComponent

interface UserWalletListComponent : ComposableContentComponent {

    interface Factory {
        fun create(context: AppComponentContext): UserWalletListComponent
    }
}
