package com.tangem.features.yield.supply.api.promobottomsheet

import com.tangem.core.decompose.factory.ComponentFactory
import com.tangem.core.ui.decompose.ComposableBottomSheetComponent

interface YieldModePromoBottomSheetComponent : ComposableBottomSheetComponent {

    data class Params(
        val offer: YieldModePromoOffer,
        val onDismiss: () -> Unit,
    )

    interface Factory : ComponentFactory<Params, YieldModePromoBottomSheetComponent>
}