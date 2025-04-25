package com.tangem.features.nft.details.info

import com.tangem.core.decompose.factory.ComponentFactory
import com.tangem.core.ui.decompose.ComposableBottomSheetComponent
import com.tangem.core.ui.extensions.TextReference

internal interface NFTDetailsInfoComponent : ComposableBottomSheetComponent {

    data class Params(
        val title: TextReference,
        val text: TextReference,
        val onDismiss: () -> Unit,
    )

    interface Factory : ComponentFactory<Params, NFTDetailsInfoComponent>
}