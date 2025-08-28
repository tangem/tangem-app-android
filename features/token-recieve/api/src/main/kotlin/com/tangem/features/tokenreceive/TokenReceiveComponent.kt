package com.tangem.features.tokenreceive

import com.tangem.core.decompose.context.AppComponentContext
import com.tangem.core.decompose.factory.ComponentFactory
import com.tangem.core.ui.decompose.ComposableBottomSheetComponent
import com.tangem.domain.models.TokenReceiveConfig

interface TokenReceiveComponent : ComposableBottomSheetComponent {

    data class Params(
        val config: TokenReceiveConfig,
        val onDismiss: () -> Unit,
    )

    interface Factory : ComponentFactory<Params, TokenReceiveComponent> {
        override fun create(context: AppComponentContext, params: Params): TokenReceiveComponent
    }
}