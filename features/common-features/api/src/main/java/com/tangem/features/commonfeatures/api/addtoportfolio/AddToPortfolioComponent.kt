package com.tangem.features.commonfeatures.api.addtoportfolio

import com.tangem.core.decompose.factory.ComponentFactory
import com.tangem.core.ui.decompose.ComposableBottomSheetComponent

interface AddToPortfolioComponent : ComposableBottomSheetComponent {

    data class Params(
        val addToPortfolioManager: AddToPortfolioManager,
    )

    interface Factory : ComponentFactory<Params, AddToPortfolioComponent>
}