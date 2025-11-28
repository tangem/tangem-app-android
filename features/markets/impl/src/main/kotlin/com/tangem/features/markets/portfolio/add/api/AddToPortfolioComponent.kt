package com.tangem.features.markets.portfolio.add.api

import com.tangem.core.decompose.factory.ComponentFactory
import com.tangem.core.ui.decompose.ComposableBottomSheetComponent

internal interface AddToPortfolioComponent : ComposableBottomSheetComponent {

    data class Params(
        val addToPortfolioManager: AddToPortfolioManager,
        val callback: Callback,
    )

    interface Callback {
        fun onDismiss()
    }

    interface Factory : ComponentFactory<Params, AddToPortfolioComponent>
}